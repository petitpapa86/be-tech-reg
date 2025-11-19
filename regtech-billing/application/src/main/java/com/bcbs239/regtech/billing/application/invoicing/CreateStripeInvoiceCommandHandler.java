package com.bcbs239.regtech.billing.application.invoicing;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.events.StripeInvoiceCreatedEvent;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.repositories.InvoiceRepository;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionRepository;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.application.saga.SagaManager;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.Map;

/**
 * Command handler for creating Stripe invoices.
 * Handles CreateStripeInvoiceCommand, creates local invoice, and publishes StripeInvoiceCreatedEvent.
 */
@Component
public class CreateStripeInvoiceCommandHandler {

    private final PaymentService paymentService;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SagaManager sagaManager;
    private static final Logger log = LoggerFactory.getLogger(CreateStripeInvoiceCommandHandler.class);

    public CreateStripeInvoiceCommandHandler(
            PaymentService paymentService,
            InvoiceRepository invoiceRepository,
            SubscriptionRepository subscriptionRepository,
            SagaManager sagaManager) {
        this.paymentService = paymentService;
        this.invoiceRepository = invoiceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.sagaManager = sagaManager;
    }

    /**
     * Handle the CreateStripeInvoiceCommand
     * NOT_SUPPORTED ensures no transaction context exists
     */
    @EventListener
    @Async("sagaTaskExecutor")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handle(CreateStripeInvoiceCommand command) {
        // Diagnostic log
        try {
            log.info("CREATE_STRIPE_INVOICE_COMMAND_RECEIVED; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "customerId", String.valueOf(command.getCustomerId()),
                "subscriptionId", String.valueOf(command.getSubscriptionId()),
                "amount", String.valueOf(command.getAmount())
            ));
        } catch (Exception e) {
            // ignore logging failures
        }

        // Convert string to StripeCustomerId
        Result<StripeCustomerId> customerIdResult = StripeCustomerId.fromString(command.getCustomerId());
        if (customerIdResult.isFailure()) {
            log.error("INVALID_CUSTOMER_ID; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "customerId", String.valueOf(command.getCustomerId()),
                "error", String.valueOf(customerIdResult.getError())
            ));
            return;
        }
        
        // Create invoice using PaymentService
        PaymentService.InvoiceCreationRequest request = new PaymentService.InvoiceCreationRequest(
            customerIdResult.getValue().get(),
            command.getAmount(),
            command.getDescription()
        );

        Result<PaymentService.InvoiceCreationResult> invoiceResult = paymentService.createInvoice(request);
        if (invoiceResult.isFailure()) {
            String errorMsg = invoiceResult.getError()
                .map(err -> err.getMessage())
                .orElse("Unknown error");
            log.error("CREATE_INVOICE_FAILED; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "customerId", String.valueOf(command.getCustomerId()),
                "errorMessage", errorMsg
            ));
            return;
        }

        PaymentService.InvoiceCreationResult stripeInvoice = invoiceResult.getValue().get();

        // Get subscription to retrieve billing account
        Result<SubscriptionId> subscriptionIdResult = SubscriptionId.fromString(command.getSubscriptionId());
        if (subscriptionIdResult.isFailure()) {
            log.error("INVALID_SUBSCRIPTION_ID; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "subscriptionId", String.valueOf(command.getSubscriptionId())
            ));
            return;
        }

        Maybe<Subscription> subscriptionMaybe = subscriptionRepository.findById(subscriptionIdResult.getValue().get());
        if (subscriptionMaybe.isEmpty()) {
            log.error("SUBSCRIPTION_NOT_FOUND; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "subscriptionId", String.valueOf(command.getSubscriptionId())
            ));
            return;
        }

        Subscription subscription = subscriptionMaybe.getValue();
        Maybe<BillingAccountId> billingAccountId = subscription.getBillingAccountId();

        // Create local Invoice aggregate
        Result<StripeInvoiceId> stripeInvoiceIdResult = StripeInvoiceId.fromString(stripeInvoice.invoiceId());
        if (stripeInvoiceIdResult.isFailure()) {
            log.error("INVALID_STRIPE_INVOICE_ID; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "invoiceId", String.valueOf(stripeInvoice.invoiceId())
            ));
            return;
        }

        // Parse amount and create Money object (Stripe amounts are in cents)
        Money totalAmount;
        try {
            // Parse as BigDecimal first to handle decimal formats like "50000.0000"
            BigDecimal amountInCents = new BigDecimal(stripeInvoice.amount());
            // Convert cents to dollars (divide by 100)
            BigDecimal amountInDollars = amountInCents.divide(BigDecimal.valueOf(100));
            totalAmount = Money.of(amountInDollars, Currency.getInstance("USD"));
        } catch (Exception e) {
            log.error("INVALID_AMOUNT; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "amount", String.valueOf(stripeInvoice.amount())
            ), e);
            return;
        }

        // Create billing period (current month)
        YearMonth currentMonth = YearMonth.now();
        BillingPeriod billingPeriod = BillingPeriod.of(currentMonth);

        // Create invoice with subscription amount and zero overage
        Money zeroOverage = Money.of(BigDecimal.ZERO, Currency.getInstance("USD"));
        Result<Invoice> invoiceCreateResult = Invoice.create(
            billingAccountId,
            stripeInvoiceIdResult.getValue().get(),
            totalAmount,
            zeroOverage,
            billingPeriod,
            Instant::now,
            LocalDate::now
        );

        if (invoiceCreateResult.isFailure()) {
            log.error("INVOICE_CREATION_FAILED; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "error", String.valueOf(invoiceCreateResult.getError())
            ));
            return;
        }

        Invoice invoice = invoiceCreateResult.getValue().get();

        // Save invoice to database
        Result<InvoiceId> saveResult = invoiceRepository.save(invoice);
        if (saveResult.isFailure()) {
            String errorMsg = saveResult.getError()
                .map(err -> err.getMessage())
                .orElse("Unknown error");
            log.error("SAVE_INVOICE_FAILED; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "errorMessage", errorMsg
            ));
            return;
        }

        InvoiceId invoiceId = saveResult.getValue().get();

        log.info("INVOICE_SAVED; details={}", Map.of(
            "sagaId", String.valueOf(command.sagaId()),
            "invoiceId", String.valueOf(invoiceId.value()),
            "stripeInvoiceId", String.valueOf(stripeInvoice.invoiceId())
        ));

        // Notify saga of invoice creation
        StripeInvoiceCreatedEvent event = new StripeInvoiceCreatedEvent(
            command.sagaId(),
            stripeInvoice.invoiceId()
        );

        try {
            sagaManager.processEvent(event);
            log.info("STRIPE_INVOICE_CREATED_PROCESSED_BY_SAGAMANAGER; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "invoiceId", String.valueOf(stripeInvoice.invoiceId())
            ));
        } catch (Exception e) {
            log.error("SAGA_PROCESSING_FAILED; details={}", Map.of(
                "sagaId", String.valueOf(command.sagaId()),
                "invoiceId", String.valueOf(stripeInvoice.invoiceId())
            ), e);
        }
    }
}
