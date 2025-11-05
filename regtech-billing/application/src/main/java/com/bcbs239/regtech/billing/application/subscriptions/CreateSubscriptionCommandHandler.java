package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.application.createsubscription.CreateSubscriptionCommand;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountRepository;
import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.repositories.SubscriptionRepository;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Function;

/**
 * Command handler for creating new subscriptions.
 * Uses functional programming patterns with closure-based dependency injection.
 */
@Component
public class CreateSubscriptionCommandHandler {

    private final BillingAccountRepository billingAccountRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;

    public CreateSubscriptionCommandHandler(
            BillingAccountRepository billingAccountRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentService paymentService) {
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentService = paymentService;
    }

    /**
     * Handle the CreateSubscriptionCommand by injecting repository operations as closures
     */
    public Result<CreateSubscriptionResponse> handle(CreateSubscriptionCommand command) {
        return createSubscription(
            command,
            billingAccountRepository,
            subscriptionRepository,
            this::createStripeSubscription
        );
    }

    /**
     * Pure function for subscription creation with injected dependencies as closures.
     * This function contains no side effects and can be easily tested.
     */
    static Result<CreateSubscriptionResponse> createSubscription(
            CreateSubscriptionCommand command,
            BillingAccountRepository billingAccountRepository,
            SubscriptionRepository subscriptionRepository,
            Function<StripeCustomerAndTier, Result<PaymentService.SubscriptionCreationResult>> stripeSubscriptionCreator) {

        // Step 1: Validate billing account ID
        Result<BillingAccountId> billingAccountIdResult = BillingAccountId.fromString(command.getBillingAccountId());
        if (billingAccountIdResult.isFailure()) {
            return Result.failure(billingAccountIdResult.getError().get());
        }
        BillingAccountId billingAccountId = billingAccountIdResult.getValue().get();

        // Step 2: Find and validate billing account
        Maybe<BillingAccount> billingAccountMaybe = billingAccountRepository.findById(billingAccountId);
        if (billingAccountMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_NOT_FOUND", 
                "Billing account not found: " + billingAccountId, "subscription.billing.account.not.found"));
        }
        BillingAccount billingAccount = billingAccountMaybe.getValue();

        // Step 3: Verify billing account can create subscriptions
        if (!billingAccount.canCreateSubscription()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_INVALID_STATUS", 
                String.format("Billing account status %s does not allow subscription creation", 
                    billingAccount.getStatus()), "subscription.billing.account.invalid.status"));
        }

        // Step 4: Check for existing active subscription
        Maybe<Subscription> existingSubscription = subscriptionRepository.findActiveByBillingAccountId(billingAccountId);
        if (existingSubscription.isPresent()) {
            return Result.failure(ErrorDetail.of("ACTIVE_SUBSCRIPTION_EXISTS", 
                "Billing account already has an active subscription", "subscription.active.already.exists"));
        }

        // Step 5: Create Stripe subscription
        StripeCustomerAndTier subscriptionData = new StripeCustomerAndTier(
            billingAccount.getStripeCustomerId().getValue(), 
            command.tier()
        );
        Result<PaymentService.SubscriptionCreationResult> stripeSubscriptionResult = stripeSubscriptionCreator.apply(subscriptionData);
        if (stripeSubscriptionResult.isFailure()) {
            return Result.failure(stripeSubscriptionResult.getError().get());
        }
        PaymentService.SubscriptionCreationResult stripeSubscription = stripeSubscriptionResult.getValue().get();

        // Step 6: Create subscription domain object
        Result<com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId> stripeSubIdResult = 
            com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId.fromString(stripeSubscription.subscriptionId());
        if (stripeSubIdResult.isFailure()) {
            return Result.failure(stripeSubIdResult.getError().get());
        }
        
        Subscription subscription = Subscription.create(
            stripeSubIdResult.getValue().get(),
            command.tier()
        );

        // Step 7: Save subscription
        Result<SubscriptionId> saveResult = subscriptionRepository.save(subscription);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        // Step 8: Calculate next billing date (first day of next month)
        LocalDate nextBillingDate = YearMonth.now().plusMonths(1).atDay(1);

        // Step 9: Return success response
        return Result.success(CreateSubscriptionResponse.of(
            subscription.getId(),
            subscription.getTier(),
            subscription.getStatus(),
            subscription.getMonthlyAmount(),
            subscription.getStartDate(),
            nextBillingDate
        ));
    }

    /**
     * Create subscription using payment service
     */
    private Result<PaymentService.SubscriptionCreationResult> createStripeSubscription(StripeCustomerAndTier data) {
        PaymentService.SubscriptionCreationRequest request = new PaymentService.SubscriptionCreationRequest(
            data.customerId(),
            data.tier(),
            null // PaymentMethodId - may need to be passed from command
        );
        return paymentService.createSubscription(request);
    }

    // Helper record for function parameters
    public record StripeCustomerAndTier(StripeCustomerId customerId, SubscriptionTier tier) {}
}
