package com.bcbs239.regtech.billing.domain.subscriptions;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.util.Objects;

/**
 * Typed wrapper for Stripe Subscription ID
 */
public record StripeSubscriptionId(String value) {
    
    public StripeSubscriptionId {
        Objects.requireNonNull(value, "StripeSubscriptionId value cannot be null");
    }
    
    /**
     * Create StripeSubscriptionId from string value with validation
     */
    public static Result<StripeSubscriptionId> fromString(String value) {
        if (value == null) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_SUBSCRIPTION_ID", "StripeSubscriptionId value cannot be null"));
        }
        if (value.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_SUBSCRIPTION_ID", "StripeSubscriptionId value cannot be empty"));
        }
        if (!value.startsWith("sub_")) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_SUBSCRIPTION_ID", "StripeSubscriptionId must start with 'sub_'"));
        }
        return Result.success(new StripeSubscriptionId(value));
    }
    
    @Override
    public String toString() {
        return value;
    }
}