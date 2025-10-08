package com.bcbs239.regtech.billing.application.commands;

import com.bcbs239.regtech.billing.domain.aggregates.BillingAccount;
import com.bcbs239.regtech.billing.domain.aggregates.Subscription;
import com.bcbs239.regtech.billing.domain.aggregates.Invoice;
import com.bcbs239.regtech.billing.domain.valueobjects.*;
import com.bcbs239.regtech.billing.domain.events.PaymentVerifiedEvent;
import com.bcbs239.regtech.billing.domain.events.InvoiceGeneratedEvent;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaInvoiceRepository;
import com.bcbs239.regtech.billing.infrastructure.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.stripe.StripeCustomer;
import com.bcbs239.regtech.billing.infrastructure.stripe.StripeSubscription;
import com.bcbs239.regtech.billing.infrastructure.events.BillingEventPublisher;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.iam.domain.users.UserId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Command handler for processing payment information during user registration.
 * Uses functional programming patterns with closure-based dependency injection.
 */
@Component
public class ProcessPaymentCommandHandler {

    private final JpaBillingAccountRepository billingAccountRepository;
    private final JpaSubscriptionRepository subscriptionRepository;
    private final JpaInvoiceRepository invoiceRepository;
    private final StripeService stripeService;
    private final BillingEventPublisher eventPublisher;

    public ProcessPaymentCommandHandler(
            JpaBillingAccountRepository billingAccountRepository,
            JpaSubscriptionRepository subscriptionRepository,
            JpaInvoiceRepository invoiceRepository,
            StripeService stripeService,
            BillingEventPublisher eventPublisher) {
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.stripeService = stripeService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handle the ProcessPaymentCommand by injecting repository operations as closures
     */
    public Result<ProcessPaymentResponse> handle(ProcessPaymentCommand command) {
        return processPayment(
            command,
            billingAccountRepository.billingAccountSaver(),
            subscriptionRepository.subscriptionSaver(),
            invoiceRepository.invoiceSaver(),
            event -> eventPublisher.publishEvent(event),
            this::extractUserDataFromSaga,
            this::createStripeCustomer,
            this::createStripeSubscription
        );
    }

    /**
     * Pure function for payment processing with injected dependencies as closures.
     * This function contains no side effects and can be easily tested.
     */
    static Result<ProcessPaymentResponse> processPayment(
            ProcessPaymentCommand command,
            Function<BillingAccount, Result<BillingAccountId>> billingAccountSaver,
            Function<Subscription, Result<SubscriptionId>> subscriptionSaver,
            Function<Invoice, Result<InvoiceId>> invoiceSaver,
            Consumer<Object> eventPublisher,
            Function<String, Result<UserData>> userDataExtractor,
            Function<UserDataAndPaymentMethod, Result<StripeCustomer>> stripeCustomerCreator,
            Function<StripeCustomerAndTier, Result<StripeSubscription>> stripeSubscriptionCreator) {

        // Step 1: Extract user data from correlation ID via saga lookup
        Result<UserData> userDataResult = userDataExtractor.apply(command.correlationId());
        if (userDataResult.isFailure()) {
            return Result.failure(userDataResult.getError().get());
        }
        UserData userData = userDataResult.getValue().get();

        // Step 2: Create Stripe customer with payment method
        UserDataAndPaymentMethod customerData = new UserDataAndPaymentMethod(userData, command.getPaymentMethodId());
        Result<StripeCustomer> stripeCustomerResult = stripeCustomerCreator.apply(customerData);
        if (stripeCustomerResult.isFailure()) {
            return Result.failure(stripeCustomerResult.getError().get());
        }
        StripeCustomer stripeCustomer = stripeCustomerResult.getValue().get();

        // Step 3: Create billing account
        BillingAccount billingAccount = BillingAccount.create(userData.userId(), stripeCustomer.customerId());
        
        Result<Void> activationResult = billingAccount.activate(command.getPaymentMethodId());
        if (activationResult.isFailure()) {
            return Result.failure(activationResult.getError().get());
        }

        Result<BillingAccountId> saveAccountResult = billingAccountSaver.apply(billingAccount);
        if (saveAccountResult.isFailure()) {
            return Result.failure(saveAccountResult.getError().get());
        }

        // Step 4: Create Stripe subscription
        StripeCustomerAndTier subscriptionData = new StripeCustomerAndTier(stripeCustomer.customerId(), SubscriptionTier.STARTER);
        Result<StripeSubscription> stripeSubscriptionResult = stripeSubscriptionCreator.apply(subscriptionData);
        if (stripeSubscriptionResult.isFailure()) {
            return Result.failure(stripeSubscriptionResult.getError().get());
        }
        StripeSubscription stripeSubscription = stripeSubscriptionResult.getValue().get();

        // Step 5: Create subscription domain object
        Subscription subscription = Subscription.create(
            billingAccount.getId(),
            stripeSubscription.subscriptionId(),
            SubscriptionTier.STARTER
        );

        Result<SubscriptionId> saveSubscriptionResult = subscriptionSaver.apply(subscription);
        if (saveSubscriptionResult.isFailure()) {
            return Result.failure(saveSubscriptionResult.getError().get());
        }

        // Step 6: Generate pro-rated first invoice
        Result<Invoice> invoiceResult = generateProRatedInvoice(
            billingAccount.getId(),
            stripeSubscription.invoiceId(),
            SubscriptionTier.STARTER
        );
        if (invoiceResult.isFailure()) {
            return Result.failure(invoiceResult.getError().get());
        }
        Invoice invoice = invoiceResult.getValue().get();

        Result<InvoiceId> saveInvoiceResult = invoiceSaver.apply(invoice);
        if (saveInvoiceResult.isFailure()) {
            return Result.failure(saveInvoiceResult.getError().get());
        }

        // Step 7: Publish domain events
        eventPublisher.accept(new PaymentVerifiedEvent(
            userData.userId(),
            billingAccount.getId(),
            command.correlationId()
        ));

        eventPublisher.accept(new InvoiceGeneratedEvent(
            invoice.getId(),
            billingAccount.getId(),
            invoice.getTotalAmount(),
            command.correlationId()
        ));

        // Step 8: Return success response
        return Result.success(ProcessPaymentResponse.of(
            billingAccount.getId(),
            subscription.getId(),
            invoice.getId(),
            invoice.getTotalAmount(),
            command.correlationId()
        ));
    }

    /**
     * Extract user data from enhanced correlation ID.
     * Format: "user-registration-{uuid}|userId={userId}|email={email}|name={name}|bankId={bankId}"
     */
    private Result<UserData> extractUserDataFromSaga(String correlationId) {
        try {
            // Parse correlation ID to extract user information
            if (!correlationId.startsWith("user-registration-")) {
                return Result.failure(ErrorDetail.of("INVALID_CORRELATION_ID", 
                    "Invalid correlation ID format: " + correlationId, "payment.correlation.id.invalid"));
            }
            
            // Split by pipe to get the data parts
            String[] parts = correlationId.split("\\|");
            if (parts.length < 4) {
                return Result.failure(ErrorDetail.of("INVALID_CORRELATION_ID", 
                    "Correlation ID missing required data: " + correlationId, "payment.correlation.id.invalid"));
            }
            
            // Extract data from key=value pairs
            String userIdValue = null;
            String emailValue = null;
            String nameValue = null;
            
            for (int i = 1; i < parts.length; i++) {
                String[] keyValue = parts[i].split("=", 2);
                if (keyValue.length == 2) {
                    switch (keyValue[0]) {
                        case "userId" -> userIdValue = keyValue[1];
                        case "email" -> emailValue = keyValue[1];
                        case "name" -> nameValue = keyValue[1];
                        // bankId is available but not needed for UserData
                    }
                }
            }
            
            if (userIdValue == null || emailValue == null || nameValue == null) {
                return Result.failure(ErrorDetail.of("INVALID_CORRELATION_ID", 
                    "Correlation ID missing required user data: " + correlationId, "payment.correlation.id.invalid"));
            }
            
            UserId userId = UserId.fromString(userIdValue);
            UserData userData = UserData.of(userId, emailValue, nameValue);
            
            return Result.success(userData);
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("SAGA_LOOKUP_FAILED", 
                "Failed to extract user data from correlation ID: " + e.getMessage(), "payment.saga.lookup.failed"));
        }
    }

    /**
     * Create Stripe customer with payment method attachment
     */
    private Result<StripeCustomer> createStripeCustomer(UserDataAndPaymentMethod data) {
        // Create Stripe customer
        Result<StripeCustomer> customerResult = stripeService.createCustomer(
            data.userData().email(), 
            data.userData().name()
        );
        if (customerResult.isFailure()) {
            return customerResult;
        }
        
        StripeCustomer customer = customerResult.getValue().get();
        
        // Attach payment method
        Result<Void> attachResult = stripeService.attachPaymentMethod(
            customer.customerId(), 
            data.paymentMethodId()
        );
        if (attachResult.isFailure()) {
            return Result.failure(attachResult.getError().get());
        }
        
        // Set as default payment method
        Result<Void> defaultResult = stripeService.setDefaultPaymentMethod(
            customer.customerId(), 
            data.paymentMethodId()
        );
        if (defaultResult.isFailure()) {
            return Result.failure(defaultResult.getError().get());
        }
        
        return Result.success(customer);
    }

    /**
     * Create Stripe subscription with billing anchor to next month
     */
    private Result<StripeSubscription> createStripeSubscription(StripeCustomerAndTier data) {
        return stripeService.createSubscription(data.customerId(), data.tier());
    }

    /**
     * Generate pro-rated invoice for the current billing period
     */
    private static Result<Invoice> generateProRatedInvoice(
            BillingAccountId billingAccountId,
            StripeInvoiceId stripeInvoiceId,
            SubscriptionTier tier) {
        
        BillingPeriod currentPeriod = BillingPeriod.forMonth(YearMonth.now());
        Money monthlyAmount = tier.getMonthlyPrice();
        
        return Invoice.createProRated(
            billingAccountId,
            stripeInvoiceId,
            monthlyAmount,
            Money.zero(Currency.getInstance("EUR")), // No overage for first invoice
            currentPeriod,
            LocalDate.now(), // Service start date
            () -> java.time.Instant.now(), // Clock supplier
            () -> LocalDate.now() // Date supplier
        );
    }

    // Helper records for function parameters
    public record UserDataAndPaymentMethod(UserData userData, PaymentMethodId paymentMethodId) {}
    public record StripeCustomerAndTier(StripeCustomerId customerId, SubscriptionTier tier) {}
}