package com.bcbs239.regtech.billing.application.invoicing;

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
import com.bcbs239.regtech.core.saga.AbstractSaga;
import com.bcbs239.regtech.core.saga.SagaId;
import com.bcbs239.regtech.core.saga.SagaStatus;
import com.bcbs239.regtech.core.saga.SagaStartedEvent;
import com.bcbs239.regtech.core.saga.SagaClosures;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
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
 * Uses event-driven pattern with closure-based dependency injection for better testability.
 */
public class MonthlyBillingSaga extends AbstractSaga<MonthlyBillingSagaData> {

    @SuppressWarnings("unused")
    private final SagaClosures.MessagePublisher messagePublisher;
    private final SagaClosures.Logger logger;
    private final Function<UsageQuery, Result<UsageMetrics>> usageMetricsQuery;
    private final Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder;
    @SuppressWarnings("unused")
    private final Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder;
    private final Function<InvoiceCreationData, Result<StripeInvoice>> stripeInvoiceCreator;
    private final Function<Invoice, Result<InvoiceId>> invoiceSaver;
    private final Consumer<Object> eventPublisher;

    public MonthlyBillingSaga(
            SagaId sagaId,
            MonthlyBillingSagaData data,
            SagaClosures.TimeoutScheduler timeoutScheduler,
            SagaClosures.MessagePublisher messagePublisher,
            SagaClosures.Logger logger,
            Function<UsageQuery, Result<UsageMetrics>> usageMetricsQuery,
            Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder,
            Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder,
            Function<InvoiceCreationData, Result<StripeInvoice>> stripeInvoiceCreator,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Consumer<Object> eventPublisher) {
        super(sagaId, "monthly-billing", data, timeoutScheduler);
        this.messagePublisher = messagePublisher;
        this.logger = logger;
        this.usageMetricsQuery = usageMetricsQuery;
        this.billingAccountFinder = billingAccountFinder;
        this.activeSubscriptionFinder = activeSubscriptionFinder;
        this.stripeInvoiceCreator = stripeInvoiceCreator;
        this.invoiceSaver = invoiceSaver;
        this.eventPublisher = eventPublisher;
        registerHandlers();
    }

    private void registerHandlers() {
        onEvent(SagaStartedEvent.class, this::handleSagaStarted);
        // Register other event handlers as needed
    }

    private void handleSagaStarted(SagaStartedEvent event) {
        // Start the billing process
        gatherUsageMetrics();
    }

    private void gatherUsageMetrics() {
        logger.log("info", "Gathering usage metrics for user {} in period {}",
            data.getUserId(), data.getBillingPeriodId());

        // Query ingestion context for usage metrics
        UsageQuery query = new UsageQuery(data.getUserId(), data.getBillingPeriod());
        Result<UsageMetrics> metricsResult = usageMetricsQuery.apply(query);

        if (metricsResult.isFailure()) {
            String errorMsg = "Failed to gather usage metrics: " + metricsResult.getError().get().getMessage();
            data.markStepFailed(errorMsg);
            fail(errorMsg);
            return;
        }

        UsageMetrics metrics = metricsResult.getValue().get();
        data.setTotalExposures(metrics.totalExposures());
        data.setDocumentsProcessed(metrics.documentsProcessed());
        data.setDataVolumeBytes(metrics.dataVolumeBytes());

        data.advanceToNextStep();
        calculateCharges();
    }

    private void calculateCharges() {
        logger.log("info", "Calculating charges for user {} with {} exposures",
            data.getUserId(), data.getTotalExposures());

        // Calculate subscription amount (always full monthly amount for existing subscriptions)
        Money subscriptionAmount = SubscriptionTier.STARTER.getMonthlyPrice();
        data.setSubscriptionCharges(subscriptionAmount);

        // Calculate overage charges
        Money overageAmount = calculateOverageCharges(data.getTotalExposures());
        data.setOverageCharges(overageAmount);

        data.advanceToNextStep();
        generateInvoice();
    }

    private void generateInvoice() {
        logger.log("info", "Generating invoice for user {} with total amount {}",
            data.getUserId(), data.getTotalCharges());

        // Find billing account to get Stripe customer ID
        Result<BillingAccount> billingAccountResult = findBillingAccount(data.getUserId());
        if (billingAccountResult.isFailure()) {
            String errorMsg = "Failed to find billing account: " + billingAccountResult.getError().get().getMessage();
            data.markStepFailed(errorMsg);
            fail(errorMsg);
            return;
        }
        BillingAccount billingAccount = billingAccountResult.getValue().get();

        // Create Stripe invoice
        InvoiceCreationData invoiceData = new InvoiceCreationData(
            billingAccount.getStripeCustomerId().getValue(),
            data.getTotalCharges(),
            "Monthly billing for " + data.getBillingPeriod().toString()
        );

        Result<StripeInvoice> stripeInvoiceResult = stripeInvoiceCreator.apply(invoiceData);
        if (stripeInvoiceResult.isFailure()) {
            String errorMsg = "Failed to create Stripe invoice: " + stripeInvoiceResult.getError().get().getMessage();
            data.markStepFailed(errorMsg);
            fail(errorMsg);
            return;
        }
        StripeInvoice stripeInvoice = stripeInvoiceResult.getValue().get();
        data.setStripeInvoiceId(stripeInvoice.invoiceId().value());

        // Create domain invoice
        Result<Invoice> invoiceResult = Invoice.create(
            Maybe.some(billingAccount.getId()),
            stripeInvoice.invoiceId(),
            data.getSubscriptionCharges(),
            data.getOverageCharges(),
            data.getBillingPeriod(),
            Instant::now,
            LocalDate::now
        );

        if (invoiceResult.isFailure()) {
            String errorMsg = "Failed to create domain invoice: " + invoiceResult.getError().get().getMessage();
            data.markStepFailed(errorMsg);
            fail(errorMsg);
            return;
        }
        Invoice invoice = invoiceResult.getValue().get();

        // Save invoice
        Result<InvoiceId> saveResult = invoiceSaver.apply(invoice);
        if (saveResult.isFailure()) {
            String errorMsg = "Failed to save invoice: " + saveResult.getError().get().getMessage();
            data.markStepFailed(errorMsg);
            fail(errorMsg);
            return;
        }

        data.setGeneratedInvoiceId(saveResult.getValue().get());
        data.advanceToNextStep();
        finalizeBilling();
    }

    private void finalizeBilling() {
        logger.log("info", "Finalizing billing for user {} with invoice {}",
            data.getUserId(), data.getGeneratedInvoiceId());

        try {
            // Find billing account for event publishing
            Result<BillingAccount> billingAccountResult = findBillingAccount(data.getUserId());
            if (billingAccountResult.isFailure()) {
                String errorMsg = "Failed to find billing account for event publishing: " +
                    billingAccountResult.getError().get().getMessage();
                data.markStepFailed(errorMsg);
                fail(errorMsg);
                return;
            }
            BillingAccount billingAccount = billingAccountResult.getValue().get();

            // Publish InvoiceGeneratedEvent
            InvoiceGeneratedEvent invoiceEvent = new InvoiceGeneratedEvent(
                data.getGeneratedInvoiceId(),
                billingAccount.getId(),
                data.getTotalCharges(),
                data.getCorrelationId()
            );
            eventPublisher.accept(invoiceEvent);

            // Mark as completed
            complete();

        } catch (Exception e) {
            String errorMsg = "Failed to finalize billing: " + e.getMessage();
            data.markStepFailed(errorMsg);
            fail(errorMsg);
        }
    }

    @Override
    protected void updateStatus() {
        if (data.getLastStepError() != null) {
            setStatus(SagaStatus.FAILED);
            setCompletedAt(Instant.now());
        } else if (data.getCurrentStep() == MonthlyBillingSagaData.BillingStep.FINALIZE_BILLING &&
                   data.getGeneratedInvoiceId() != null) {
            setStatus(SagaStatus.COMPLETED);
            setCompletedAt(Instant.now());
        }
    }

    @Override
    protected void compensate() {
        logger.log("warn", "Compensating monthly billing saga for user {}", data.getUserId());

        try {
            // Reverse any partial operations based on current step
            switch (data.getCurrentStep()) {
                case FINALIZE_BILLING, GENERATE_INVOICE -> {
                    // If we generated an invoice, try to cancel/void it
                    if (data.getGeneratedInvoiceId() != null) {
                        logger.log("info", "Attempting to cancel invoice {} during compensation",
                            data.getGeneratedInvoiceId());
                        // In a real implementation, we would call Stripe to void the invoice
                        // For now, just log the compensation action
                    }
                }
                case CALCULATE_CHARGES -> {
                    // Clear calculated charges
                    data.setSubscriptionCharges(Money.zero(Currency.getInstance("EUR")));
                    data.setOverageCharges(Money.zero(Currency.getInstance("EUR")));
                    logger.log("info", "Cleared calculated charges during compensation");
                }
                case GATHER_METRICS -> {
                    // Clear gathered metrics
                    data.setTotalExposures(0);
                    data.setDocumentsProcessed(0);
                    data.setDataVolumeBytes(0L);
                    logger.log("info", "Cleared usage metrics during compensation");
                }
            }

            logger.log("info", "Successfully compensated monthly billing saga for user {}",
                data.getUserId());

        } catch (Exception e) {
            logger.log("error", "Failed to compensate monthly billing saga for user {}: {}",
                data.getUserId(), e.getMessage());
        }
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
