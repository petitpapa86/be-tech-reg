package com.bcbs239.regtech.billing.application.sagas;

import com.bcbs239.regtech.billing.application.shared.UsageMetrics;
import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceGeneratedEvent;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice;
import com.bcbs239.regtech.core.saga.SagaMessage;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Monthly billing saga that orchestrates the billing process for active subscriptions.
 * Uses closure-based dependency injection for better testability and functional programming.
 */
public class MonthlyBillingSaga implements Saga<MonthlyBillingSagaData> {

    // Closure dependencies for external operations
    private final SagaClosures.MessagePublisher messagePublisher;
    private final SagaClosures.Logger logger;
    private final Function<UsageQuery, Result<UsageMetrics>> usageMetricsQuery;
    private final Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder;
    private final Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder;
    private final Function<InvoiceCreationData, Result<StripeInvoice>> stripeInvoiceCreator;
    private final Function<Invoice, Result<InvoiceId>> invoiceSaver;
    private final Consumer<Object> eventPublisher;

    public MonthlyBillingSaga(
            SagaClosures.MessagePublisher messagePublisher,
            SagaClosures.Logger logger,
            Function<UsageQuery, Result<UsageMetrics>> usageMetricsQuery,
            Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder,
            Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder,
            Function<InvoiceCreationData, Result<StripeInvoice>> stripeInvoiceCreator,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Consumer<Object> eventPublisher) {
        this.messagePublisher = messagePublisher;
        this.logger = logger;
        this.usageMetricsQuery = usageMetricsQuery;
        this.billingAccountFinder = billingAccountFinder;
        this.activeSubscriptionFinder = activeSubscriptionFinder;
        this.stripeInvoiceCreator = stripeInvoiceCreator;
        this.invoiceSaver = invoiceSaver;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SagaResult execute(MonthlyBillingSagaData sagaData) {
        try {
            logger.log("info", "Executing monthly billing saga step: {} for user: {}", 
                sagaData.getCurrentStep(), sagaData.getUserId());

            return switch (sagaData.getCurrentStep()) {
                case GATHER_METRICS -> gatherUsageMetrics(sagaData);
                case CALCULATE_CHARGES -> calculateCharges(sagaData);
                case GENERATE_INVOICE -> generateInvoice(sagaData);
                case FINALIZE_BILLING -> finalizeBilling(sagaData);
            };
        } catch (Exception e) {
            logger.log("error", "Monthly billing saga failed for user {}: {}", 
                sagaData.getUserId(), e.getMessage());
            sagaData.markStepFailed("Unexpected error: " + e.getMessage());
            return SagaResult.failure("Monthly billing failed: " + e.getMessage());
        }
    }

    /**
     * Step 1: Gather usage metrics from ingestion context
     */
    private SagaResult gatherUsageMetrics(MonthlyBillingSagaData sagaData) {
        logger.log("info", "Gathering usage metrics for user {} in period {}", 
            sagaData.getUserId(), sagaData.getBillingPeriodId());

        // Query ingestion context for usage metrics
        UsageQuery query = new UsageQuery(sagaData.getUserId(), sagaData.getBillingPeriod());
        Result<UsageMetrics> metricsResult = usageMetricsQuery.apply(query);
        
        if (metricsResult.isFailure()) {
            String errorMsg = "Failed to gather usage metrics: " + metricsResult.getError().get().getMessage();
            sagaData.markStepFailed(errorMsg);
            return SagaResult.failure(errorMsg);
        }

        UsageMetrics metrics = metricsResult.getValue().get();
        sagaData.setTotalExposures(metrics.totalExposures());
        sagaData.setDocumentsProcessed(metrics.documentsProcessed());
        sagaData.setDataVolumeBytes(metrics.dataVolumeBytes());
        
        sagaData.advanceToNextStep();

        logger.log("info", "Gathered usage metrics for user {}: {} exposures, {} documents", 
            sagaData.getUserId(), metrics.totalExposures(), metrics.documentsProcessed());

        return SagaResult.success();
    }

    /**
     * Step 2: Calculate subscription and overage charges
     */
    private SagaResult calculateCharges(MonthlyBillingSagaData sagaData) {
        logger.log("info", "Calculating charges for user {} with {} exposures", 
            sagaData.getUserId(), sagaData.getTotalExposures());

        // Calculate subscription amount (always full monthly amount for existing subscriptions)
        Money subscriptionAmount = SubscriptionTier.STARTER.getMonthlyPrice();
        sagaData.setSubscriptionCharges(subscriptionAmount);

        // Calculate overage charges
        Money overageAmount = calculateOverageCharges(sagaData.getTotalExposures());
        sagaData.setOverageCharges(overageAmount);

        sagaData.advanceToNextStep();

        logger.log("info", "Calculated charges for user {}: subscription={}, overage={}, total={}", 
            sagaData.getUserId(), subscriptionAmount, overageAmount, sagaData.getTotalCharges());

        return SagaResult.success();
    }

    /**
     * Step 3: Generate invoice through Stripe integration
     */
    private SagaResult generateInvoice(MonthlyBillingSagaData sagaData) {
        logger.log("info", "Generating invoice for user {} with total amount {}", 
            sagaData.getUserId(), sagaData.getTotalCharges());

        // Find billing account to get Stripe customer ID
        Result<BillingAccount> billingAccountResult = findBillingAccount(sagaData.getUserId());
        if (billingAccountResult.isFailure()) {
            String errorMsg = "Failed to find billing account: " + billingAccountResult.getError().get().getMessage();
            sagaData.markStepFailed(errorMsg);
            return SagaResult.failure(errorMsg);
        }
        BillingAccount billingAccount = billingAccountResult.getValue().get();

        // Create Stripe invoice
        InvoiceCreationData invoiceData = new InvoiceCreationData(
            billingAccount.getStripeCustomerId(),
            sagaData.getTotalCharges(),
            "Monthly billing for " + sagaData.getBillingPeriod().toString()
        );

        Result<StripeInvoice> stripeInvoiceResult = stripeInvoiceCreator.apply(invoiceData);
        if (stripeInvoiceResult.isFailure()) {
            String errorMsg = "Failed to create Stripe invoice: " + stripeInvoiceResult.getError().get().getMessage();
            sagaData.markStepFailed(errorMsg);
            return SagaResult.failure(errorMsg);
        }
        StripeInvoice stripeInvoice = stripeInvoiceResult.getValue().get();
        sagaData.setStripeInvoiceId(stripeInvoice.invoiceId().value());

        // Create domain invoice
        Result<Invoice> invoiceResult = Invoice.create(
            billingAccount.getId(),
            stripeInvoice.invoiceId(),
            sagaData.getSubscriptionCharges(),
            sagaData.getOverageCharges(),
            sagaData.getBillingPeriod(),
            Instant::now,
            LocalDate::now
        );

        if (invoiceResult.isFailure()) {
            String errorMsg = "Failed to create domain invoice: " + invoiceResult.getError().get().getMessage();
            sagaData.markStepFailed(errorMsg);
            return SagaResult.failure(errorMsg);
        }
        Invoice invoice = invoiceResult.getValue().get();

        // Save invoice
        Result<InvoiceId> saveResult = invoiceSaver.apply(invoice);
        if (saveResult.isFailure()) {
            String errorMsg = "Failed to save invoice: " + saveResult.getError().get().getMessage();
            sagaData.markStepFailed(errorMsg);
            return SagaResult.failure(errorMsg);
        }

        sagaData.setGeneratedInvoiceId(saveResult.getValue().get());
        sagaData.advanceToNextStep();

        logger.log("info", "Generated invoice {} for user {} with Stripe invoice {}", 
            saveResult.getValue().get(), sagaData.getUserId(), stripeInvoice.invoiceId());

        return SagaResult.success();
    }

    /**
     * Step 4: Finalize billing process and publish events
     */
    private SagaResult finalizeBilling(MonthlyBillingSagaData sagaData) {
        logger.log("info", "Finalizing billing for user {} with invoice {}", 
            sagaData.getUserId(), sagaData.getGeneratedInvoiceId());

        try {
            // Find billing account for event publishing
            Result<BillingAccount> billingAccountResult = findBillingAccount(sagaData.getUserId());
            if (billingAccountResult.isFailure()) {
                String errorMsg = "Failed to find billing account for event publishing: " + 
                    billingAccountResult.getError().get().getMessage();
                sagaData.markStepFailed(errorMsg);
                return SagaResult.failure(errorMsg);
            }
            BillingAccount billingAccount = billingAccountResult.getValue().get();

            // Publish InvoiceGeneratedEvent
            InvoiceGeneratedEvent invoiceEvent = new InvoiceGeneratedEvent(
                sagaData.getGeneratedInvoiceId(),
                billingAccount.getId(),
                sagaData.getTotalCharges(),
                sagaData.getCorrelationId()
            );
            eventPublisher.accept(invoiceEvent);

            // Publish saga completion message to other contexts
            SagaMessage completionMessage = SagaMessage.builder()
                .sagaId(sagaData.getSagaId())
                .type("billing.monthly-billing-completed")
                .source("billing-context")
                .target("iam-context")
                .payload(new BillingCompletedPayload(
                    sagaData.getUserId(),
                    sagaData.getGeneratedInvoiceId(),
                    sagaData.getTotalCharges(),
                    sagaData.getBillingPeriodId(),
                    sagaData.getTotalExposures()
                ))
                .build();

            messagePublisher.publish(completionMessage);

            logger.log("info", "Completed monthly billing for user {} - invoice: {}, total: {}", 
                sagaData.getUserId(), sagaData.getGeneratedInvoiceId(), sagaData.getTotalCharges());

            return SagaResult.success();

        } catch (Exception e) {
            String errorMsg = "Failed to finalize billing: " + e.getMessage();
            sagaData.markStepFailed(errorMsg);
            return SagaResult.failure(errorMsg);
        }
    }

    @Override
    public SagaResult handleMessage(MonthlyBillingSagaData sagaData, SagaMessage message) {
        logger.log("info", "Handling message {} for monthly billing saga {}", 
            message.getType(), sagaData.getSagaId());

        // Handle any incoming messages during billing process
        // For monthly billing, we typically don't expect many external messages
        // but this could handle things like usage metric updates or payment confirmations
        
        return switch (message.getType()) {
            case "usage.metrics-updated" -> handleUsageMetricsUpdate(sagaData, message);
            case "payment.confirmed" -> handlePaymentConfirmation(sagaData, message);
            default -> {
                logger.log("warn", "Unhandled message type {} in monthly billing saga", message.getType());
                yield SagaResult.success(); // Ignore unknown messages
            }
        };
    }

    @Override
    public SagaResult compensate(MonthlyBillingSagaData sagaData) {
        logger.log("warn", "Compensating monthly billing saga for user {}", sagaData.getUserId());
        
        try {
            // Reverse any partial operations based on current step
            switch (sagaData.getCurrentStep()) {
                case FINALIZE_BILLING, GENERATE_INVOICE -> {
                    // If we generated an invoice, try to cancel/void it
                    if (sagaData.getGeneratedInvoiceId() != null) {
                        logger.log("info", "Attempting to cancel invoice {} during compensation", 
                            sagaData.getGeneratedInvoiceId());
                        // In a real implementation, we would call Stripe to void the invoice
                        // For now, just log the compensation action
                    }
                }
                case CALCULATE_CHARGES -> {
                    // Clear calculated charges
                    sagaData.setSubscriptionCharges(Money.zero(Currency.getInstance("EUR")));
                    sagaData.setOverageCharges(Money.zero(Currency.getInstance("EUR")));
                    logger.log("info", "Cleared calculated charges during compensation");
                }
                case GATHER_METRICS -> {
                    // Clear gathered metrics
                    sagaData.setTotalExposures(0);
                    sagaData.setDocumentsProcessed(0);
                    sagaData.setDataVolumeBytes(0L);
                    logger.log("info", "Cleared usage metrics during compensation");
                }
            }

            logger.log("info", "Successfully compensated monthly billing saga for user {}", 
                sagaData.getUserId());
            return SagaResult.success();

        } catch (Exception e) {
            logger.log("error", "Failed to compensate monthly billing saga for user {}: {}", 
                sagaData.getUserId(), e.getMessage());
            return SagaResult.failure("Compensation failed: " + e.getMessage());
        }
    }

    @Override
    public String getSagaType() {
        return "monthly-billing";
    }

    // Helper methods

    /**
     * Calculate overage charges based on exposure count
     */
    private Money calculateOverageCharges(int totalExposures) {
        int exposureLimit = SubscriptionTier.STARTER.getExposureLimit();
        
        if (totalExposures <= exposureLimit) {
            return Money.zero(Currency.getInstance("EUR"));
        }

        int overageExposures = totalExposures - exposureLimit;
        // €0.05 per exposure over limit
        BigDecimal overageRate = new BigDecimal("0.05");
        BigDecimal overageAmount = overageRate.multiply(BigDecimal.valueOf(overageExposures));
        
        return Money.of(overageAmount, Currency.getInstance("EUR"));
    }

    /**
     * Find billing account by user ID
     */
    private Result<BillingAccount> findBillingAccount(UserId userId) {
        // This would typically use a repository method to find by user ID
        // For now, we'll create a mock implementation
        // In real implementation, this would be injected as a closure
        
        // Mock billing account ID - in real implementation this would be looked up
        BillingAccountId mockBillingAccountId = BillingAccountId.fromString("billing-account-" + userId.getValue())
            .getValue().orElse(null);
        
        if (mockBillingAccountId == null) {
            return Result.failure(ErrorDetail.of("INVALID_BILLING_ACCOUNT_ID", 
                "Could not create billing account ID for user: " + userId, "billing.account.id.invalid"));
        }

        Maybe<BillingAccount> billingAccountMaybe = billingAccountFinder.apply(mockBillingAccountId);
        if (billingAccountMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND", 
                "Billing account not found for user: " + userId, "billing.account.not.found"));
        }

        return Result.success(billingAccountMaybe.getValue());
    }

    /**
     * Handle usage metrics update message
     */
    private SagaResult handleUsageMetricsUpdate(MonthlyBillingSagaData sagaData, SagaMessage message) {
        logger.log("info", "Handling usage metrics update for saga {}", sagaData.getSagaId());
        
        // If we're still in the metrics gathering phase, we might want to re-query
        if (sagaData.getCurrentStep() == MonthlyBillingSagaData.BillingStep.GATHER_METRICS) {
            return gatherUsageMetrics(sagaData);
        }
        
        // Otherwise, just acknowledge the message
        return SagaResult.success();
    }

    /**
     * Handle payment confirmation message
     */
    private SagaResult handlePaymentConfirmation(MonthlyBillingSagaData sagaData, SagaMessage message) {
        logger.log("info", "Handling payment confirmation for saga {}", sagaData.getSagaId());
        
        // Payment confirmation might trigger finalization if we're waiting for it
        if (sagaData.getCurrentStep() == MonthlyBillingSagaData.BillingStep.FINALIZE_BILLING) {
            return finalizeBilling(sagaData);
        }
        
        return SagaResult.success();
    }

    // Helper records for function parameters
    public record UsageQuery(UserId userId, BillingPeriod billingPeriod) {}
    public record InvoiceCreationData(StripeCustomerId customerId, Money amount, String description) {}
    public record BillingCompletedPayload(
        UserId userId,
        InvoiceId invoiceId,
        Money totalAmount,
        String billingPeriodId,
        int totalExposures
    ) {}
}
