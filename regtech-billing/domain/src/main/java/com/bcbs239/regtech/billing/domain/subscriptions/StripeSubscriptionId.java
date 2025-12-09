package com.bcbs239.regtech.billing.domain.subscriptions;



import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

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
            return Result.failure("INVALID_STRIPE_SUBSCRIPTION_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeSubscriptionId value cannot be null", null);
        }
        // normalize whitespace for subsequent checks
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return Result.failure("INVALID_STRIPE_SUBSCRIPTION_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeSubscriptionId value cannot be empty", null);
        }
        // special allowed literal
        if (normalized.equalsIgnoreCase("default")) {
            return Result.success(new StripeSubscriptionId(normalized));
        }
        if (!normalized.startsWith("sub_")) {
            return Result.failure("INVALID_STRIPE_SUBSCRIPTION_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeSubscriptionId must start with 'sub_'", null);
        }

        return Result.success(new StripeSubscriptionId(normalized));
    }

}

