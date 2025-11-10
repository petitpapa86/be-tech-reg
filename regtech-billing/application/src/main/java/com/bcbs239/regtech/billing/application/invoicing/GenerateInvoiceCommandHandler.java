package com.bcbs239.regtech.billing.application.invoicing;

import com.bcbs239.regtech.billing.application.shared.UsageMetrics;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.repositories.InvoiceRepository;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionRepository;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Command handler for manually generating invoices.
 * Uses functional programming patterns with closure-based dependency injection.
 */
@Component
public class GenerateInvoiceCommandHandler {

    private final BillingAccountRepository billingAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;

    public GenerateInvoiceCommandHandler(
            BillingAccountRepository billingAccountRepository,
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository,
            PaymentService paymentService) {
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentService = paymentService;
    }

    /**
     * Handle the GenerateInvoiceCommand using direct repository method calls
     */
    public Result<GenerateInvoiceResponse> handle(GenerateInvoiceCommand command) {
        // Step 1: Validate billing account ID
        Result<BillingAccountId> billingAccountIdResult = command.getBillingAccountId();
        if (billingAccountIdResult.isFailure()) {
            return Result.failure(billingAccountIdResult.getError().get());
        }
        BillingAccountId billingAccountId = billingAccountIdResult.getValue().get();

        // Step 2: Find and validate billing account
        Maybe<BillingAccount> billingAccountMaybe = billingAccountRepository.findById(billingAccountId);
        if (billingAccountMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND", ErrorType.BUSINESS_RULE_ERROR,
                "Billing account not found: " + billingAccountId, "invoice.billing.account.not.found"));
        }
        BillingAccount billingAccount = billingAccountMaybe.getValue();

        // Step 3: Find active subscription
        Maybe<Subscription> subscriptionMaybe = subscriptionRepository.findActiveByBillingAccountId(billingAccountId);
        if (subscriptionMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("NO_ACTIVE_SUBSCRIPTION", ErrorType.BUSINESS_RULE_ERROR,
                "No active subscription found for billing account: " + billingAccountId, "invoice.no.active.subscription"));
        }
        Subscription subscription = subscriptionMaybe.getValue();

        // Step 4: Query usage metrics from ingestion context
        UsageQuery usageQuery = new UsageQuery(billingAccount.getUserId(), command.billingPeriod());
        Result<UsageMetrics> usageResult = queryUsageMetrics(usageQuery);
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
        Result<PaymentService.InvoiceCreationResult> invoiceResult = createStripeInvoice(invoiceData);
        if (invoiceResult.isFailure()) {
            return Result.failure(invoiceResult.getError().get());
        }
        PaymentService.InvoiceCreationResult createdInvoice = invoiceResult.getValue().get();

        // Step 9: Create invoice domain object
        Result<com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId> stripeInvoiceIdResult = 
            com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId.fromString(createdInvoice.invoiceId());
        if (stripeInvoiceIdResult.isFailure()) {
            return Result.failure(stripeInvoiceIdResult.getError().get());
        }
        
        Result<Invoice> domainInvoiceResult = Invoice.create(
            Maybe.some(billingAccountId),
            stripeInvoiceIdResult.getValue().get(),
            subscriptionAmount,
            overageAmount,
            command.billingPeriod(),
            () -> Instant.now(), // Clock supplier
            () -> LocalDate.now() // Date supplier
        );
        if (domainInvoiceResult.isFailure()) {
            return Result.failure(domainInvoiceResult.getError().get());
        }
        Invoice invoice = domainInvoiceResult.getValue().get();

        // Step 10: Save invoice
        Result<InvoiceId> saveResult = invoiceRepository.save(invoice);
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
            return Result.failure(ErrorDetail.of("USAGE_METRICS_QUERY_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Failed to query usage metrics: " + e.getMessage(), "invoice.usage.metrics.query.failed"));
        }
    }

    /**
     * Create invoice using payment service
     */
    private Result<PaymentService.InvoiceCreationResult> createStripeInvoice(InvoiceCreationData data) {
        PaymentService.InvoiceCreationRequest request = new PaymentService.InvoiceCreationRequest(
            data.customerId(),
            data.amount().amount().toString(),
            data.description()
        );
        return paymentService.createInvoice(request);
    }

    // Helper records for function parameters
    public record UsageQuery(com.bcbs239.regtech.iam.domain.users.UserId userId, BillingPeriod billingPeriod) {}
    public record InvoiceCreationData(StripeCustomerId customerId, Money amount, String description) {}
}

