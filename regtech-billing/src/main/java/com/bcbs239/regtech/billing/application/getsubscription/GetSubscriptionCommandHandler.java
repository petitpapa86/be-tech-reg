package com.bcbs239.regtech.billing.application.getsubscription;

import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.infrastructure.database.repositories.JpaSubscriptionRepository;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Command handler for retrieving subscription details.
 * Uses functional programming patterns with closure-based dependency injection.
 */
@Component
public class GetSubscriptionCommandHandler {

    private final JpaSubscriptionRepository subscriptionRepository;

    public GetSubscriptionCommandHandler(JpaSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Handle the GetSubscriptionCommand by injecting repository operations as closures
     */
    public Result<GetSubscriptionResponse> handle(GetSubscriptionCommand command) {
        return getSubscription(
            command,
            subscriptionRepository.subscriptionFinder()
        );
    }

    /**
     * Pure function for subscription retrieval with injected dependencies as closures.
     * This function contains no side effects and can be easily tested.
     */
    static Result<GetSubscriptionResponse> getSubscription(
            GetSubscriptionCommand command,
            Function<SubscriptionId, Maybe<Subscription>> subscriptionFinder) {

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
        Subscription subscription = subscriptionMaybe.getValue();

        // Step 3: Return subscription details
        return Result.success(GetSubscriptionResponse.from(subscription));
    }
}
