package com.bcbs239.regtech.billing.domain.subscriptions;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for Subscription aggregate
 */
public record SubscriptionId(String value) {
    
    public SubscriptionId {
        Objects.requireNonNull(value, "SubscriptionId value cannot be null");
    }
    
    /**
     * Generate a new unique SubscriptionId
     */
    public static SubscriptionId generate() {
        return new SubscriptionId("subscription-" + UUID.randomUUID().toString());
    }
    
    /**
     * Create SubscriptionId from string value with validation
     */
    public static Result<SubscriptionId> fromString(String value) {
        if (value == null) {
            return Result.failure(new ErrorDetail("INVALID_SUBSCRIPTION_ID", "SubscriptionId value cannot be null"));
        }
        if (value.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_SUBSCRIPTION_ID", "SubscriptionId value cannot be empty"));
        }
        return Result.success(new SubscriptionId(value));
    }

}

