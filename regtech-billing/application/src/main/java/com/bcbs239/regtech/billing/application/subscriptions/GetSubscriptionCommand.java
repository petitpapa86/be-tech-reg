package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.validation.constraints.NotBlank;

/**
 * Command for retrieving subscription details by ID.
 * Contains subscription ID for lookup.
 */
public record GetSubscriptionCommand(
    @NotBlank(message = "Subscription ID is required")
    String subscriptionId
) {
    
    /**
     * Factory method to create and validate GetSubscriptionCommand
     */
    public static Result<GetSubscriptionCommand> create(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_ID_REQUIRED", 
                "Subscription ID is required", "subscription.id.required"));
        }
        
        return Result.success(new GetSubscriptionCommand(subscriptionId.trim()));
    }
    
    /**
     * Get SubscriptionId value object
     */
    public Result<SubscriptionId> getSubscriptionId() {
        return SubscriptionId.fromString(subscriptionId);
    }
}
