package com.bcbs239.regtech.billing.application.commands;

import com.bcbs239.regtech.billing.domain.aggregates.Subscription;
import com.bcbs239.regtech.billing.domain.valueobjects.SubscriptionId;
import com.bcbs239.regtech.billing.infrastructure.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.billing.infrastructure.stripe.StripeService;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Command handler for cancelling subscriptions.
 * Uses functional programming patterns with closure-based dependency injection.
 */
@Component
public class CancelSubscriptionCommandHandler {

    private final JpaSubscriptionRepository subscriptionRepository;
    private final StripeService stripeService;

    public CancelSubscriptionCommandHandler(
            JpaSubscriptionRepository subscriptionRepository,
            StripeService stripeService) {
        this.subscriptionRepository = subscriptionRepository;
        this.stripeService = stripeService;
    }

    /**
     * Handle the CancelSubscriptionCommand by injecting repository operations as closures
     */
    public Result<CancelSubscriptionResponse> handle(CancelSubscriptionCommand command) {
        return cancelSubscription(
            command,
            subscriptionRepository.subscriptionFinder(),
            subscriptionRepository.subscriptionSaver(),
            this::cancelStripeSubscription
        );
    }

    /**
     * Pure function for subscription cancellation with injected dependencies as closures.
     * This function contains no side effects and can be easily tested.
     */
    static Result<CancelSubscriptionResponse> cancelSubscription(
            CancelSubscriptionCommand command,
            Function<SubscriptionId, Maybe<Subscription>> subscriptionFinder,
            Function<Subscription, Result<SubscriptionId>> subscriptionSaver,
            Function<Subscription, Result<Void>> stripeCanceller) {

        // Step 1: Validate subscription ID
        Result<SubscriptionId> subscriptionIdResult = command.getSubscriptionId();
        if (subscriptionIdResult.isFailure()) {
            return Result.failure(subscriptionIdResult.getError().get());
        }
        SubscriptionId subscriptionId = subscriptionIdResult.getValue().get();

        // Step 2: Find subscription
        Maybe<Subscription> subscriptionMaybe = subscriptionFinder.apply(subscriptionId);
        if (subscriptionMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_NOT_FOUND", 
                "Subscription not found: " + subscriptionId, "subscription.not.found"));
        }
        Subscription subscription = subscriptionMaybe.get();

        // Step 3: Cancel subscription in domain
        Result<Void> cancellationResult = subscription.cancel(command.getEffectiveCancellationDate());
        if (cancellationResult.isFailure()) {
            return Result.failure(cancellationResult.getError().get());
        }

        // Step 4: Cancel subscription in Stripe
        Result<Void> stripeCancellationResult = stripeCanceller.apply(subscription);
        if (stripeCancellationResult.isFailure()) {
            return Result.failure(stripeCancellationResult.getError().get());
        }

        // Step 5: Save updated subscription
        Result<SubscriptionId> saveResult = subscriptionSaver.apply(subscription);
        if (saveResult.isFailure()) {
            return Result.failure(saveResult.getError().get());
        }

        // Step 6: Return success response
        return Result.success(CancelSubscriptionResponse.of(
            subscription.getId(),
            subscription.getStatus(),
            subscription.getEndDate()
        ));
    }

    /**
     * Cancel subscription in Stripe
     */
    private Result<Void> cancelStripeSubscription(Subscription subscription) {
        return stripeService.cancelSubscription(subscription.getStripeSubscriptionId());
    }
}