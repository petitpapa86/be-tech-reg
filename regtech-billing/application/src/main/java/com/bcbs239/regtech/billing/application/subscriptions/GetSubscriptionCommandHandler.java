package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.repositories.SubscriptionRepository;
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

    private final SubscriptionRepository subscriptionRepository;

    public GetSubscriptionCommandHandler(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Handle the GetSubscriptionCommand using direct repository method calls
     */
    public Result<GetSubscriptionResponse> handle(GetSubscriptionCommand command) {
        // Step 1: Validate subscription ID
        Result<SubscriptionId> subscriptionIdResult = command.getSubscriptionId();
        if (subscriptionIdResult.isFailure()) {
            return Result.failure(subscriptionIdResult.getError().get());
        }
        SubscriptionId subscriptionId = subscriptionIdResult.getValue().get();

        // Step 2: Find subscription
        Maybe<Subscription> subscriptionMaybe = subscriptionRepository.findById(subscriptionId);
        if (subscriptionMaybe.isEmpty()) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_NOT_FOUND", 
                "Subscription not found: " + subscriptionId, "subscription.not.found"));
        }
        Subscription subscription = subscriptionMaybe.getValue();

        // Step 3: Return subscription details
        return Result.success(GetSubscriptionResponse.from(subscription));
    }
}
