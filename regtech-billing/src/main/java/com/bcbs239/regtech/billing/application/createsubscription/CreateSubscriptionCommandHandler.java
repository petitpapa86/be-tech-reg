package com.bcbs239.regtech.billing.application.createsubscription;

import com.bcbs239.regtech.billing.application.createsubscription.CreateSubscriptionCommand;
import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaBillingAccountRepository;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeService;
import com.bcbs239.regtech.billing.infrastructure.external.stripe.StripeSubscription;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
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

    private final JpaBillingAccountRepository billingAccountRepository;
    private final JpaSubscriptionRepository subscriptionRepository;
    private final StripeService stripeService;

    public CreateSubscriptionCommandHandler(
            JpaBillingAccountRepository billingAccountRepository,
            JpaSubscriptionRepository subscriptionRepository,
            StripeService stripeService) {
        this.billingAccountRepository = billingAccountRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeService = stripeService;
    }

    /**
     * Handle the CreateSubscriptionCommand by injecting repository operations as closures
     */
    public Result<CreateSubscriptionResponse> handle(CreateSubscriptionCommand command) {
        return createSubscription(
            command,
            billingAccountRepository.billingAccountFinder(),
            subscriptionRepository.activeSubscriptionFinder(),
            subscriptionRepository.subscriptionSaver(),
            this::createStripeSubscription
        );
    }

    /**
     * Pure function for subscription creation with injected dependencies as closures.
     * This function contains no side effects and can be easily tested.
     */
    static Result<CreateSubscriptionResponse> createSubscription(
            CreateSubscriptionCommand command,
            Function<BillingAccountId, Maybe<BillingAccount>> billingAccountFinder,
            Function<BillingAccountId, Maybe<Subscription>> activeSubscriptionFinder,
            Function<Subscription, Result<SubscriptionId>> subscriptionSaver,
            Function<StripeCustomerAndTier, Result<StripeSubscription>> stripeSubscriptionCreator) {

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
        Maybe<Subscription> existingSubscription = activeSubscriptionFinder.apply(billingAccountId);
        if (existingSubscription.isPresent()) {
            return Result.failure(ErrorDetail.of("ACTIVE_SUBSCRIPTION_EXISTS", 
                "Billing account already has an active subscription", "subscription.active.already.exists"));
        }

        // Step 5: Create Stripe subscription
        StripeCustomerAndTier subscriptionData = new StripeCustomerAndTier(
            billingAccount.getStripeCustomerId().getValue(), 
            command.tier()
        );
        Result<StripeSubscription> stripeSubscriptionResult = stripeSubscriptionCreator.apply(subscriptionData);
        if (stripeSubscriptionResult.isFailure()) {
            return Result.failure(stripeSubscriptionResult.getError().get());
        }
        StripeSubscription stripeSubscription = stripeSubscriptionResult.getValue().get();

        // Step 6: Create subscription domain object
        Subscription subscription = Subscription.create(
            billingAccountId,
            stripeSubscription.subscriptionId(),
            command.tier()
        );

        // Step 7: Save subscription
        Result<SubscriptionId> saveResult = subscriptionSaver.apply(subscription);
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
     * Create Stripe subscription with billing anchor to next month
     */
    private Result<StripeSubscription> createStripeSubscription(StripeCustomerAndTier data) {
        return stripeService.createSubscription(data.customerId(), data.tier());
    }

    // Helper record for function parameters
    public record StripeCustomerAndTier(StripeCustomerId customerId, SubscriptionTier tier) {}
}
