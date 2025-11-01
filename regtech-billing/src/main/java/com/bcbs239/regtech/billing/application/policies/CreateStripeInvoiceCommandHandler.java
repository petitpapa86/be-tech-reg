package com.bcbs239.regtech.billing.application.policies;

import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.domain.events.StripeInvoiceCreatedEvent;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Maybe;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Command handler for creating Stripe invoices in the saga pattern.
 * Retrieves Stripe invoice data and creates domain invoice objects.
 */
@Component
public class CreateStripeInvoiceCommandHandler {

    private final JpaInvoiceRepository invoiceRepository;
    private final StripeService stripeService;
    private final BillingEventPublisher eventPublisher;

    public CreateStripeInvoiceCommandHandler(
            JpaInvoiceRepository invoiceRepository,
            StripeService stripeService,
            BillingEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.stripeService = stripeService;
        this.eventPublisher = eventPublisher;
    }

 
    public Result<Void> handle(CreateStripeInvoiceCommand command) {
        return processInvoiceCreation(
            command,
            invoiceRepository.invoiceSaver(),
            eventPublisher::publishEvent,
            this::retrieveStripeInvoice,
            this::createDomainInvoice
        );
    }

    /**
     * Pure function for invoice creation with injected dependencies
     */
    static Result<Void> processInvoiceCreation(
            CreateStripeInvoiceCommand command,
            java.util.function.Function<Invoice, Result<com.bcbs239.regtech.billing.domain.invoices.InvoiceId>> invoiceSaver,
            java.util.function.Consumer<Object> eventPublisher,
            java.util.function.Function<String, Result<com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice>> stripeInvoiceRetriever,
            java.util.function.Function<InvoiceCreationData, Result<Invoice>> domainInvoiceCreator) {

        // Step 1: Retrieve Stripe invoice data
        Result<com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice> stripeInvoiceResult =
            stripeInvoiceRetriever.apply(command.getStripeInvoiceId());
        if (stripeInvoiceResult.isFailure()) {
            return Result.failure(stripeInvoiceResult.getError().get());
        }

        // Step 2: Create domain invoice using pro-rated logic
        InvoiceCreationData creationData = new InvoiceCreationData(
            BillingAccountId.fromString(command.getBillingAccountId()).getValue().get(),
            StripeInvoiceId.fromString(command.getStripeInvoiceId()).getValue().get(),
            SubscriptionTier.STARTER // Default to STARTER tier for now
        );

        Result<Invoice> invoiceResult = domainInvoiceCreator.apply(creationData);
        if (invoiceResult.isFailure()) {
            return Result.failure(invoiceResult.getError().get());
        }
        Invoice invoice = invoiceResult.getValue().get();

        // Step 3: Save the invoice
        Result<com.bcbs239.regtech.billing.domain.invoices.InvoiceId> saveResult = invoiceSaver.apply(invoice);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        // Step 4: Publish domain event
        eventPublisher.accept(new StripeInvoiceCreatedEvent(command.getSagaId(), command.getStripeInvoiceId()));

        return Result.success(null);
    }

    /**
     * Retrieve Stripe invoice data
     */
    private Result<com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeInvoice> retrieveStripeInvoice(String stripeInvoiceId) {
        return StripeInvoiceId.fromString(stripeInvoiceId)
            .flatMap(stripeService::retrieveInvoice);
    }

    /**
     * Create domain invoice using pro-rated logic (similar to ProcessPaymentCommandHandler)
     */
    private Result<Invoice> createDomainInvoice(InvoiceCreationData data) {
        BillingPeriod currentPeriod = BillingPeriod.forMonth(YearMonth.now());
        Money monthlyAmount = data.tier.getMonthlyPrice();

        return Invoice.createProRated(
            Maybe.some(data.billingAccountId),
            data.stripeInvoiceId,
            monthlyAmount,
            Money.zero(monthlyAmount.currency()), // No overage for first invoice
            currentPeriod,
            LocalDate.now(), // Service start date
            () -> java.time.Instant.now(), // Clock supplier
            () -> LocalDate.now() // Date supplier
        );
    }

    /**
     * Data class for invoice creation parameters
     */
    private record InvoiceCreationData(
        BillingAccountId billingAccountId,
        StripeInvoiceId stripeInvoiceId,
        SubscriptionTier tier
    ) {}
}