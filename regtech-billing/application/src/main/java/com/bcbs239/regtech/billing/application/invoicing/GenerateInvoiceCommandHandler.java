package com.bcbs239.regtech.billing.application.invoicing;

import com.bcbs239.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.billing.application.shared.UsageMetrics;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Function;

/**
 * Command handler for manually generating invoices.
 * Uses functional programming patterns with closure-based dependency injection.
 */
@Component
public class GenerateInvoiceCommandHandler {

    private final JpaBillingAccountRepository billingAccountRepository;
    private final JpaSubscriptionRepository subscriptionRepository;
    private final JpaInvoiceRepository invoiceRepository;
    private final StripeService stripeService;

    public GenerateInvoiceCommandHandler(
            JpaBillingAccountRepository billingAccountRepository,
            JpaSubscriptionRepository subscriptionRepository,
            JpaInvoiceRepository invoiceRepository,
            StripeService stripeService) {
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.stripeService = stripeService;
    }

    /**
     * Handle the GenerateInvoiceCommand by injecting repository operations as closures
     */
    public Result<GenerateInvoiceResponse> handle(GenerateInvoiceCommand command) {
        return generateInvoice(
            command,
            billingAccountRepository.billingAccountFinder(),
            subscriptionRepository.activeSubscriptionFinder(),
            invoiceRepository.invoiceSaver(),
            this::queryUsageMetrics,
            this::createStripeInvoice
        );
    }

    /**
     * Pure function for invoice generation with injected dependencies as closures.
     * This function contains no side effects and can be easily tested.
     */
    static Result<GenerateInvoiceResponse> generateInvoice(
            GenerateInvoiceCommand command,
            Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder,
            Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Function<UsageQuery, Result<UsageMetrics>> usageMetricsQuery,
            Function<InvoiceCreationData, Result<StripeInvoice>> stripeInvoiceCreator) {

        // Step 1: Validate billing account ID
        Result<BillingAccountId> billingAccountIdResult = command.getBillingAccountId();
        if (billingAccountIdResult.isFailure()) {
            return Result.failure(billingAccountIdResult.getError().get());
        }
        BillingAccountId billingAccountId = billingAccountIdResult.getValue().get();

        // Step 2: Find and validate billing account
        Maybe<BillingAccount> billingAccountMaybe = billingAccountFinder.apply(billingAccountId);
        if (billingAccountMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND", 
                "Billing account not found: " + billingAccountId, "invoice.billing.account.not.found"));
        }
        BillingAccount billingAccount = billingAccountMaybe.getValue();

        // Step 3: Find active subscription
        Maybe<Subscription> subscriptionMaybe = activeSubscriptionFinder.apply(billingAccountId);
        if (subscriptionMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("NO_ACTIVE_SUBSCRIPTION", 
                "No active subscription found for billing account: " + billingAccountId, "invoice.no.active.subscription"));
        }
        Subscription subscription = subscriptionMaybe.getValue();

        // Step 4: Query usage metrics from ingestion context
        UsageQuery usageQuery = new UsageQuery(billingAccount.getUserId(), command.billingPeriod());
        Result<UsageMetrics> usageResult = usageMetricsQuery.apply(usageQuery);
        if (usageResult.isFailure()) {
            return Result.failure(usageResult.getError().get());
        }
        UsageMetrics usage = usageResult.getValue().get();

        // Step 5: Calculate subscription amount
        Money subscriptionAmount = subscription.getMonthlyAmount();

        // Step 6: Calculate overage charges
        Money overageAmount = subscription.calculateOverageCharges(usage.totalExposures());

        // Step 7: Calculate total amount
        Result<Money> totalResult = subscriptionAmount.add(overageAmount);
        if (totalResult.isFailure()) {
            return Result.failure(totalResult.getError().get());
        }
        Money totalAmount = totalResult.getValue().get();

        // Step 8: Create Stripe invoice
        InvoiceCreationData invoiceData = new InvoiceCreationData(
            billingAccount.getStripeCustomerId().getValue(),
            totalAmount,
            "Invoice for " + command.billingPeriod().toString()
        );
        Result<StripeInvoice> stripeInvoiceResult = stripeInvoiceCreator.apply(invoiceData);
        if (stripeInvoiceResult.isFailure()) {
            return Result.failure(stripeInvoiceResult.getError().get());
        }
        StripeInvoice stripeInvoice = stripeInvoiceResult.getValue().get();

        // Step 9: Create invoice domain object
        Result<Invoice> invoiceResult = Invoice.create(
            Maybe.some(billingAccountId),
            stripeInvoice.invoiceId(),
            subscriptionAmount,
            overageAmount,
            command.billingPeriod(),
            () -> Instant.now(), // Clock supplier
            () -> LocalDate.now() // Date supplier
        );
        if (invoiceResult.isFailure()) {
            return Result.failure(invoiceResult.getError().get());
        }
        Invoice invoice = invoiceResult.getValue().get();

        // Step 10: Save invoice
        Result<InvoiceId> saveResult = invoiceSaver.apply(invoice);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        // Step 11: Return success response
        return Result.success(GenerateInvoiceResponse.of(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getStatus(),
            invoice.getSubscriptionAmount(),
            invoice.getOverageAmount(),
            invoice.getTotalAmount(),
            invoice.getBillingPeriod(),
            invoice.getIssueDate(),
            invoice.getDueDate(),
            usage.totalExposures(),
            subscription.getExposureLimit()
        ));
    }

    /**
     * Query usage metrics from ingestion context.
     * In a real implementation, this would call the ingestion context API.
     */
    private Result<UsageMetrics> queryUsageMetrics(UsageQuery query) {
        // TODO: Implement actual ingestion context integration
        // For now, return mock data for development
        try {
            // In real implementation, this would make an HTTP call or message query
            // to the ingestion context to get usage metrics for the user and period
            
            // Mock data - in production this would be actual usage data
            UsageMetrics metrics = UsageMetrics.of(
                query.userId(),
                query.billingPeriod(),
                8500, // Total exposures (under the 10,000 limit)
                150,  // Documents processed
                1024L * 1024L * 500L // 500MB data volume
            );
            
            return Result.success(metrics);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("USAGE_METRICS_QUERY_FAILED", 
                "Failed to query usage metrics: " + e.getMessage(), "invoice.usage.metrics.query.failed"));
        }
    }

    /**
     * Create Stripe invoice
     */
    private Result<StripeInvoice> createStripeInvoice(InvoiceCreationData data) {
        return stripeService.createInvoice(data.customerId(), data.amount(), data.description());
    }

    // Helper records for function parameters
    public record UsageQuery(com.bcbs239.regtech.iam.domain.users.UserId userId, BillingPeriod billingPeriod) {}
    public record InvoiceCreationData(StripeCustomerId customerId, Money amount, String description) {}
}
