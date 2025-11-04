package com.bcbs239.regtech.billing.application.payments;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.repositories.InvoiceRepository;
import com.bcbs239.regtech.billing.domain.repositories.SubscriptionRepository;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Command handler for processing payment operations.
 * Simplified version using PaymentService domain interface.
 */
@Component
public class ProcessPaymentCommandHandler {

    private final BillingAccountRepository billingAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentService paymentService;

    public ProcessPaymentCommandHandler(
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
     * Handle payment processing command
     */
    public Result<ProcessPaymentResponse> handle(ProcessPaymentCommand command) {
        // Create customer using PaymentService
        PaymentService.CustomerCreationRequest customerRequest = new PaymentService.CustomerCreationRequest(
            "user@example.com", // Would be extracted from command/saga
            "User Name", // Would be extracted from command/saga
            command.getPaymentMethodId()
        );

        Result<PaymentService.CustomerCreationResult> customerResult = paymentService.createCustomer(customerRequest);
        if (customerResult.isFailure()) {
            return Result.failure(customerResult.getError().get());
        }

        PaymentService.CustomerCreationResult customer = customerResult.getValue().get();

        // Create billing account
        BillingAccount billingAccount = new BillingAccount.Builder()
            .userId(UserId.fromString("user-123")) // Would be extracted from command/saga
            .stripeCustomerId(customer.customerId())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        Result<Void> activationResult = billingAccount.activate(command.getPaymentMethodId());
        if (activationResult.isFailure()) {
            return Result.failure(activationResult.getError().get());
        }

        // Save billing account
        Result<BillingAccountId> saveAccountResult = billingAccountRepository.save(billingAccount);
        if (saveAccountResult.isFailure()) {
            return Result.failure(saveAccountResult.getError().get());
        }

        // Create subscription
        PaymentService.SubscriptionCreationRequest subscriptionRequest = new PaymentService.SubscriptionCreationRequest(
            customer.customerId(),
            SubscriptionTier.STARTER,
            command.getPaymentMethodId()
        );

        Result<PaymentService.SubscriptionCreationResult> subscriptionResult = paymentService.createSubscription(subscriptionRequest);
        if (subscriptionResult.isFailure()) {
            return Result.failure(subscriptionResult.getError().get());
        }

        PaymentService.SubscriptionCreationResult stripeSubscription = subscriptionResult.getValue().get();

        // Create subscription domain object
        Result<com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId> stripeSubIdResult = 
            com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId.fromString(stripeSubscription.subscriptionId());
        if (stripeSubIdResult.isFailure()) {
            return Result.failure(stripeSubIdResult.getError().get());
        }
        
        Subscription subscription = Subscription.create(
            com.bcbs239.regtech.core.shared.Maybe.some(saveAccountResult.getValue().get()),
            stripeSubIdResult.getValue().get(),
            SubscriptionTier.STARTER,
            java.time.LocalDate.now()
        );

        // Save subscription
        Result<SubscriptionId> saveSubscriptionResult = subscriptionRepository.save(subscription);
        if (saveSubscriptionResult.isFailure()) {
            return Result.failure(saveSubscriptionResult.getError().get());
        }

        return Result.success(new ProcessPaymentResponse(
            saveAccountResult.getValue().get(),
            saveSubscriptionResult.getValue().get(),
            customer.customerId()
        ));
    }

    // Helper record for response
    public record ProcessPaymentResponse(
        BillingAccountId billingAccountId,
        SubscriptionId subscriptionId,
        StripeCustomerId stripeCustomerId
    ) {}
}