package com.bcbs239.regtech.billing.infrastructure.scheduling;

import com.bcbs239.regtech.billing.application.sagas.MonthlyBillingSaga;
import com.bcbs239.regtech.billing.application.sagas.MonthlyBillingSagaData;
import com.bcbs239.regtech.billing.domain.aggregates.Subscription;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.SubscriptionStatus;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.core.saga.SagaOrchestrator;
import com.bcbs239.regtech.core.saga.SagaResult;
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
import java.util.concurrent.CompletableFuture;

/**
 * Scheduled job for triggering monthly billing processes.
 * Runs on the first day of each month to start billing sagas for all active subscriptions.
 */
@Component
public class MonthlyBillingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyBillingScheduler.class);

    private final SagaOrchestrator sagaOrchestrator;
    private final MonthlyBillingSaga monthlyBillingSaga;
    private final JpaSubscriptionRepository subscriptionRepository;

    public MonthlyBillingScheduler(
            SagaOrchestrator sagaOrchestrator,
            MonthlyBillingSaga monthlyBillingSaga,
            JpaSubscriptionRepository subscriptionRepository) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.monthlyBillingSaga = monthlyBillingSaga;
        this.subscriptionRepository = subscriptionRepository;
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
                // Extract user ID from subscription (simplified implementation)
                UserId userId = extractUserIdFromSubscription(subscription);
                
                // Create saga data with correlation ID format: userId-billingPeriod
                MonthlyBillingSagaData sagaData = MonthlyBillingSagaData.create(userId, billingPeriod);
                sagaData.setId(UUID.randomUUID().toString());
                
                // Add metadata for tracking
                sagaData.addMetadata("subscriptionId", subscription.getId().getValue());
                sagaData.addMetadata("billingAccountId", subscription.getBillingAccountId().getValue());
                sagaData.addMetadata("subscriptionTier", subscription.getTier().name());
                sagaData.addMetadata("orchestrationTimestamp", java.time.Instant.now().toString());

                // Start the saga using the core orchestrator
                CompletableFuture<SagaResult> sagaFuture = sagaOrchestrator.startSaga(monthlyBillingSaga, sagaData);
                
                // For now, we'll just log the saga start - in production you might want to track these futures
                successCount++;
                logger.info("Started monthly billing saga {} for subscription {}", 
                    sagaData.getId(), subscription.getId());
                
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to start monthly billing saga for subscription {}: {}", 
                    subscription.getId(), e.getMessage());
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
     * Extract user ID from subscription (simplified implementation)
     */
    private UserId extractUserIdFromSubscription(Subscription subscription) {
        // This is a simplified implementation
        // In reality, we would need to look up the billing account to get the user ID
        // or have the user ID directly in the subscription
        
        // For now, we'll extract from the billing account ID (mock implementation)
        String billingAccountValue = subscription.getBillingAccountId().getValue();
        String userIdValue = billingAccountValue.replace("billing-account-", "");
        
        return UserId.fromString(userIdValue).getValue()
            .orElse(UserId.fromString("unknown-user-" + UUID.randomUUID()).getValue().get());
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