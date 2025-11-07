package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.payments.PaymentService;
import com.bcbs239.regtech.billing.domain.repositories.SubscriptionRepository;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import org.springframework.stereotype.Component;

/**
 * Command handler for cancelling subscriptions.
 * Uses functional programming patterns with closure-based dependency injection.
 */
@Component
public class CancelSubscriptionCommandHandler {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;

    public CancelSubscriptionCommandHandler(
            SubscriptionRepository subscriptionRepository,
            PaymentService paymentService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentService = paymentService;
    }

    /**
     * Handle the CancelSubscriptionCommand using direct repository method calls
     */
    public Result<CancelSubscriptionResponse> handle(CancelSubscriptionCommand command) {
        // Step 1: Validate subscription ID
        Result<SubscriptionId> subscriptionIdResult = command.getSubscriptionId();
        if (subscriptionIdResult.isFailure()) {
            return Result.failure(subscriptionIdResult.getError().get());
        }
        SubscriptionId subscriptionId = subscriptionIdResult.getValue().get();

        // Step 2: Find subscription
        Maybe<Subscription> subscriptionMaybe = subscriptionRepository.findById(subscriptionId);
        if (subscriptionMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_NOT_FOUND", ErrorType.BUSINESS_RULE_ERROR,
                "Subscription not found: " + subscriptionId, "subscription.not.found"));
        }
        Subscription subscription = subscriptionMaybe.getValue();

        // Step 3: Cancel subscription in domain
        Result<Void> cancellationResult = subscription.cancel(command.getEffectiveCancellationDate());
        if (cancellationResult.isFailure()) {
            return Result.failure(cancellationResult.getError().get());
        }

        // Step 4: Cancel subscription in Stripe
        Result<Void> stripeCancellationResult = cancelStripeSubscription(subscription);
        if (stripeCancellationResult.isFailure()) {
            return Result.failure(stripeCancellationResult.getError().get());
        }

        // Step 5: Save updated subscription
        Result<SubscriptionId> saveResult = subscriptionRepository.save(subscription);
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
        return paymentService.cancelSubscription(subscription.getStripeSubscriptionId().value());
    }
}

