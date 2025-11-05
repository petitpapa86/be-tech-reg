package com.bcbs239.regtech.billing.domain.subscriptions;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

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
        // normalize whitespace for subsequent checks
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_SUBSCRIPTION_ID", "StripeSubscriptionId value cannot be empty"));
        }
        // special allowed literal
        if (normalized.equalsIgnoreCase("default")) {
            return Result.success(new StripeSubscriptionId(normalized));
        }
        if (!normalized.startsWith("sub_")) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_SUBSCRIPTION_ID", "StripeSubscriptionId must start with 'sub_'"));
        }

        return Result.success(new StripeSubscriptionId(normalized));
    }

}
