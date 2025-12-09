package com.bcbs239.regtech.billing.application.invoicing;

import com.bcbs239.regtech.billing.application.shared.UsageMetrics;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceGeneratedEvent;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.domain.saga.AbstractSaga;
import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaStatus;
import com.bcbs239.regtech.core.domain.saga.TimeoutScheduler;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.CrossModuleEventBus;
import com.bcbs239.regtech.core.infrastructure.saga.SagaStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Monthly billing saga that orchestrates the billing process for active subscriptions.
 * Uses event-driven pattern with closure-based dependency injection for better testability.
 */
public class MonthlyBillingSaga extends AbstractSaga<MonthlyBillingSagaData> {

    @SuppressWarnings("unused")
    private final CrossModuleEventBus messagePublisher;
    private static final Logger log = LoggerFactory.getLogger(MonthlyBillingSaga.class);
    private final Function<UsageQuery, Result<UsageMetrics>> usageMetricsQuery;
    private final Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder;
    @SuppressWarnings("unused")
    private final Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder;
    private final Function<PaymentService.InvoiceCreationRequest, Result<PaymentService.InvoiceCreationResult>> stripeInvoiceCreator;
    private final Function<Invoice, Result<InvoiceId>> invoiceSaver;
    private final Consumer<Object> eventPublisher;

    public MonthlyBillingSaga(
            CrossModuleEventBus messagePublisher,
            SagaId sagaId,
            MonthlyBillingSagaData data,
            TimeoutScheduler timeoutScheduler,
            Function<UsageQuery, Result<UsageMetrics>> usageMetricsQuery,
            Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder,
            Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder,
            Function<PaymentService.InvoiceCreationRequest, Result<PaymentService.InvoiceCreationResult>> stripeInvoiceCreator,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Consumer<Object> eventPublisher) {
        super(sagaId, "MonthlyBillingSaga", data, timeoutScheduler);
        this.messagePublisher = messagePublisher;
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
        log.info("Gathering usage metrics for user; details={}", Map.of("userId", data.getUserId(), "billingPeriodId", data.getBillingPeriodId()));

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
        log.info("Calculating charges for user; details={}", Map.of("userId", data.getUserId(), "exposures", data.getTotalExposures()));

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
        log.info("Generating invoice for user; details={}", Map.of("userId", data.getUserId(), "totalAmount", data.getTotalCharges()));

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
        Maybe<StripeCustomerId> customerIdMaybe = billingAccount.getStripeCustomerId();
        if (customerIdMaybe.isEmpty()) {
            String errorMsg = "Billing account has no Stripe customer ID";
            data.markStepFailed(errorMsg);
            fail(errorMsg);
            return;
        }
        
        PaymentService.InvoiceCreationRequest invoiceRequest = new PaymentService.InvoiceCreationRequest(
            customerIdMaybe.getValue(),
            data.getTotalCharges().getAmount().toString(),
            "Monthly billing for " + data.getBillingPeriod()
        );

        Result<PaymentService.InvoiceCreationResult> stripeInvoiceResult = stripeInvoiceCreator.apply(invoiceRequest);
        if (stripeInvoiceResult.isFailure()) {
            String errorMsg = "Failed to create Stripe invoice: " + stripeInvoiceResult.getError().get().getMessage();
            data.markStepFailed(errorMsg);
            fail(errorMsg);
            return;
        }
        PaymentService.InvoiceCreationResult stripeInvoice = stripeInvoiceResult.getValue().get();
        data.setStripeInvoiceId(stripeInvoice.invoiceId());

        // Create domain invoice
        Result<Invoice> invoiceResult = Invoice.create(
            Maybe.some(billingAccount.getId()),
            StripeInvoiceId.fromString(stripeInvoice.invoiceId()).getValue().get(),
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
        log.info("Finalizing billing for user; details={}", Map.of("userId", data.getUserId(), "invoiceId", data.getGeneratedInvoiceId()));

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
                getId().toString()
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
        log.info("Compensating monthly billing saga for user; details={}", Map.of("userId", data.getUserId()));

        try {
            // Reverse any partial operations based on current step
            switch (data.getCurrentStep()) {
                case FINALIZE_BILLING, GENERATE_INVOICE -> {
                    // If we generated an invoice, try to cancel/void it
                    if (data.getGeneratedInvoiceId() != null) {
                        log.info("Attempting to cancel invoice during compensation; details={}", Map.of("invoiceId", data.getGeneratedInvoiceId()));
                        // In a real implementation, we would call Stripe to void the invoice
                        // For now, just log the compensation action
                    }
                }
                case CALCULATE_CHARGES -> {
                    // Clear calculated charges
                    data.setSubscriptionCharges(Money.zero(Currency.getInstance("EUR")));
                    data.setOverageCharges(Money.zero(Currency.getInstance("EUR")));
                    log.info("Cleared calculated charges during compensation; details={}", Map.of("userId", data.getUserId()));
                }
                case GATHER_METRICS -> {
                    // Clear gathered metrics
                    data.setTotalExposures(0);
                    data.setDocumentsProcessed(0);
                    data.setDataVolumeBytes(0L);
                    log.info("Cleared usage metrics during compensation; details={}", Map.of("userId", data.getUserId()));
                }
            }

            log.info("Successfully compensated monthly billing saga for user; details={}", Map.of("userId", data.getUserId()));

        } catch (Exception e) {
            log.error("Failed to compensate monthly billing saga for user; details={}", Map.of("userId", data.getUserId()), e);
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
            return Result.failure(ErrorDetail.of("INVALID_BILLING_ACCOUNT_ID", ErrorType.BUSINESS_RULE_ERROR,
                "Could not create billing account ID for user: " + userId, "billing.account.id.invalid"));
        }

        Maybe<BillingAccount> billingAccountMaybe = billingAccountFinder.apply(mockBillingAccountId);
        if (billingAccountMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND", ErrorType.BUSINESS_RULE_ERROR,
                "Billing account not found for user: " + userId, "billing.account.not.found"));
        }

        return Result.success(billingAccountMaybe.getValue());
    }

    // Helper records for function parameters
    public record UsageQuery(UserId userId, BillingPeriod billingPeriod) {}
    public record BillingCompletedPayload(
        UserId userId,
        InvoiceId invoiceId,
        Money totalAmount,
        String billingPeriodId,
        int totalExposures
    ) {}
}

