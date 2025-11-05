package com.bcbs239.regtech.billing.application.subscriptions;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Command for cancelling a subscription.
 * Contains subscription ID and optional cancellation date.
 */
public record CancelSubscriptionCommand(
    @NotBlank(message = "Subscription ID is required")
    String subscriptionId,
    
    LocalDate cancellationDate
) {
    
    /**
     * Factory method to create and validate CancelSubscriptionCommand with immediate cancellation
     */
    public static Result<CancelSubscriptionCommand> create(String subscriptionId) {
        return create(subscriptionId, LocalDate.now());
    }
    
    /**
     * Factory method to create and validate CancelSubscriptionCommand with specific cancellation date
     */
    public static Result<CancelSubscriptionCommand> create(String subscriptionId, LocalDate cancellationDate) {
        if (subscriptionId == null || subscriptionId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_ID_REQUIRED", 
                "Subscription ID is required", "subscription.id.required"));
        }
        
        LocalDate effectiveCancellationDate = cancellationDate != null ? cancellationDate : LocalDate.now();
        
        return Result.success(new CancelSubscriptionCommand(
            subscriptionId.trim(),
            effectiveCancellationDate
        ));
    }
    
    /**
     * Get SubscriptionId value object
     */
    public Result<SubscriptionId> getSubscriptionId() {
        return SubscriptionId.fromString(subscriptionId);
    }
    
    /**
     * Get effective cancellation date (defaults to today if null)
     */
    public LocalDate getEffectiveCancellationDate() {
        return cancellationDate != null ? cancellationDate : LocalDate.now();
    }
}

