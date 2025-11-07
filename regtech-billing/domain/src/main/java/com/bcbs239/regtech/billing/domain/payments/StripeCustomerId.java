package com.bcbs239.regtech.billing.domain.payments;



import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.Objects;

/**
 * Typed wrapper for Stripe Customer ID
 */
public record StripeCustomerId(String value) {
    
    public StripeCustomerId {
        Objects.requireNonNull(value, "StripeCustomerId value cannot be null");
    }
    
    /**
     * Create StripeCustomerId from string value with validation
     */
    public static Result<StripeCustomerId> fromString(String value) {
        if (value == null) {
            return Result.failure("INVALID_STRIPE_CUSTOMER_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeCustomerId value cannot be null", null);
        }
        if (value.trim().isEmpty()) {
            return Result.failure("INVALID_STRIPE_CUSTOMER_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeCustomerId value cannot be empty", null);
        }
        if (!value.startsWith("cus_")) {
            return Result.failure("INVALID_STRIPE_CUSTOMER_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeCustomerId must start with 'cus_'", null);
        }
        return Result.success(new StripeCustomerId(value));
    }
    
    /**
     * Get the value (compatibility method)
     */
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}

