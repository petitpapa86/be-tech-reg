package com.bcbs239.regtech.billing.domain.valueobjects;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
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
            return Result.failure(new ErrorDetail("INVALID_STRIPE_CUSTOMER_ID", "StripeCustomerId value cannot be null"));
        }
        if (value.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_CUSTOMER_ID", "StripeCustomerId value cannot be empty"));
        }
        if (!value.startsWith("cus_")) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_CUSTOMER_ID", "StripeCustomerId must start with 'cus_'"));
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
