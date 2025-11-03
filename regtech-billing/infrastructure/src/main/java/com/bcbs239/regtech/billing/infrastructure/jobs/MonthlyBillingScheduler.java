package com.bcbs239.regtech.billing.infrastructure.jobs;

import com.bcbs239.regtech.billing.application.policies.MonthlyBillingSaga;
import com.bcbs239.regtech.billing.application.policies.MonthlyBillingSagaData;
import com.bcbs239.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionStatus;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaManager;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.iam.domain.users.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled job for triggering monthly billing processes.
 * Runs on the first day of each month to start billing sagas for all active subscriptions.
 */
@Component
public class MonthlyBillingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyBillingScheduler.class);

    private final SagaManager sagaManager;
    private final JpaSubscriptionRepository subscriptionRepository;
    private final JpaBillingAccountRepository billingAccountRepository;

    public MonthlyBillingScheduler(
            SagaManager sagaManager,
            JpaSubscriptionRepository subscriptionRepository,
            JpaBillingAccountRepository billingAccountRepository) {
        this.sagaManager = sagaManager;
        this.subscriptionRepository = subscriptionRepository;
        this.billingAccountRepository = billingAccountRepository;
    }

    /**
     * Scheduled job that runs on the first day of each month at 00:00:00 UTC.
     * Triggers monthly billing for the previous month's usage.
     */
    @Scheduled(cron = "0 0 0 1 * ?", zone = "UTC")
    public void executeMonthlyBilling() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        YearMonth previousMonth = YearMonth.from(now.minusMonths(1));
        
        logger.info("Starting scheduled monthly billing for period: {}", previousMonth);

        try {
            MonthlyBillingResult result = startMonthlyBillingSagas(previousMonth);

            logger.info("Monthly billing orchestration completed for {}: " +
                "{} total subscriptions, {} successful sagas, {} failed sagas, success rate: {:.2f}%",
                previousMonth,
                result.totalSubscriptions(),
                result.successfulSagas(),
                result.failedSagas(),
                result.getSuccessRate() * 100);

            if (result.hasFailures()) {
                logger.warn("Monthly billing had {} failures out of {} total subscriptions",
                    result.failedSagas(), result.totalSubscriptions());
            }

        } catch (Exception e) {
            logger.error("Unexpected error during scheduled monthly billing for {}: {}",
                previousMonth, e.getMessage(), e);
        }
    }

    /**
     * Manual trigger for monthly billing (useful for testing or manual runs).
     */
    public MonthlyBillingResult triggerMonthlyBilling(YearMonth billingMonth) {
        logger.info("Manually triggering monthly billing for period: {}", billingMonth);

        try {
            MonthlyBillingResult result = startMonthlyBillingSagas(billingMonth);

            logger.info("Manual monthly billing completed for {}: " +
                "{} total subscriptions, {} successful sagas, {} failed sagas",
                billingMonth,
                result.totalSubscriptions(),
                result.successfulSagas(),
                result.failedSagas());

            return result;

        } catch (Exception e) {
            logger.error("Unexpected error during manual monthly billing for {}: {}",
                billingMonth, e.getMessage(), e);
            return new MonthlyBillingResult(
                BillingPeriod.forMonth(billingMonth), 0, 0, 1
            );
        }
    }

    /**
     * Start monthly billing sagas for all active subscriptions
     */
    private MonthlyBillingResult startMonthlyBillingSagas(YearMonth billingMonth) {
        BillingPeriod billingPeriod = BillingPeriod.forMonth(billingMonth);
        
        // Find all active subscriptions
        List<Subscription> activeSubscriptions = subscriptionRepository.findByStatusIn(
            List.of(SubscriptionStatus.ACTIVE)
        );
        
        if (activeSubscriptions.isEmpty()) {
            logger.info("No active subscriptions found for billing period: {}", billingMonth);
            return new MonthlyBillingResult(billingPeriod, 0, 0, 0);
        }

        logger.info("Found {} active subscriptions for billing", activeSubscriptions.size());

        int successCount = 0;
        int failureCount = 0;
        
        // Create saga for each active subscription
        for (Subscription subscription : activeSubscriptions) {
            try {
                // Extract user ID from subscription by looking up billing account
                Maybe<UserId> userIdMaybe = extractUserIdFromSubscription(subscription);
                
                if (userIdMaybe.isEmpty()) {
                    failureCount++;
                    logger.error("Failed to extract user ID for subscription {}: billing account not found", 
                        subscription.getId());
                    continue;
                }
                
                UserId userId = userIdMaybe.getValue();
                
                // Generate correlation ID in format: userId-billingPeriod (e.g., "user-123-2024-01")
                String correlationId = generateCorrelationId(userId, billingPeriod);
                
                // Create saga data with correlation ID
                MonthlyBillingSagaData sagaData = MonthlyBillingSagaData.create(userId, billingPeriod);
                sagaData.setId(UUID.randomUUID().toString());
                sagaData.setCorrelationId(correlationId);
                
                // Add metadata for tracking and audit
                sagaData.addMetadata("subscriptionId", subscription.getId().value());
                sagaData.addMetadata("billingAccountId", subscription.getBillingAccountId().getValue().value());
                sagaData.addMetadata("subscriptionTier", subscription.getTier().name());
                sagaData.addMetadata("orchestrationTimestamp", java.time.Instant.now().toString());
                sagaData.addMetadata("scheduledBillingPeriod", billingMonth.toString());

                // Start the saga using the saga manager
                SagaId sagaId = sagaManager.startSaga(MonthlyBillingSaga.class, sagaData);
                
                // For now, we'll just log the saga start - in production you might want to track these futures
                successCount++;
                logger.info("Started monthly billing saga {} with correlation ID {} for subscription {} (user: {})", 
                    sagaId, correlationId, subscription.getId(), userId.getValue());
                
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to start monthly billing saga for subscription {}: {}", 
                    subscription.getId(), e.getMessage(), e);
            }
        }

        return new MonthlyBillingResult(
            billingPeriod,
            activeSubscriptions.size(),
            successCount,
            failureCount
        );
    }

    /**
     * Extract user ID from subscription by looking up the billing account
     */
    private Maybe<UserId> extractUserIdFromSubscription(Subscription subscription) {
        try {
            // Look up the billing account to get the user ID
            Maybe<BillingAccount> billingAccountMaybe = billingAccountRepository
                .billingAccountFinder()
                .apply(subscription.getBillingAccountId().getValue());
            
            if (billingAccountMaybe.isEmpty()) {
                logger.warn("Billing account {} not found for subscription {}", 
                    subscription.getBillingAccountId().getValue(), subscription.getId());
                return Maybe.none();
            }
            
            BillingAccount billingAccount = billingAccountMaybe.getValue();
            return Maybe.some(billingAccount.getUserId());
            
        } catch (Exception e) {
            logger.error("Error extracting user ID for subscription {}: {}", 
                subscription.getId(), e.getMessage(), e);
            return Maybe.none();
        }
    }
    
    /**
     * Generate correlation ID in format: userId-billingPeriod (e.g., "user-123-2024-01")
     */
    public String generateCorrelationId(UserId userId, BillingPeriod billingPeriod) {
        return userId.getValue() + "-" + billingPeriod.getPeriodId();
    }

    /**
     * Trigger billing for current month (useful for testing)
     */
    public MonthlyBillingResult triggerCurrentMonthBilling() {
        YearMonth currentMonth = YearMonth.now();
        return triggerMonthlyBilling(currentMonth);
    }

    /**
     * Trigger billing for previous month (most common manual use case)
     */
    public MonthlyBillingResult triggerPreviousMonthBilling() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        return triggerMonthlyBilling(previousMonth);
    }

    /**
     * Result of monthly billing orchestration
     */
    public record MonthlyBillingResult(
        BillingPeriod billingPeriod,
        int totalSubscriptions,
        int successfulSagas,
        int failedSagas
    ) {
        public boolean hasFailures() {
            return failedSagas > 0;
        }

        public double getSuccessRate() {
            if (totalSubscriptions == 0) return 1.0;
            return (double) successfulSagas / totalSubscriptions;
        }
    }
}
