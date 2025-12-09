package com.bcbs239.regtech.billing.domain.payments;



import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.Objects;

/**
 * Typed wrapper for Payment Method ID (Stripe payment method)
 */
public record PaymentMethodId(String value) {
    
    public PaymentMethodId {
        Objects.requireNonNull(value, "PaymentMethodId value cannot be null");
    }
    
    /**
     * Create PaymentMethodId from string value with validation
     */
    public static Result<PaymentMethodId> fromString(String value) {
        if (value == null) {
            return Result.failure("INVALID_PAYMENT_METHOD_ID", ErrorType.BUSINESS_RULE_ERROR, "PaymentMethodId value cannot be null", null);
        }
        if (value.trim().isEmpty()) {
            return Result.failure("INVALID_PAYMENT_METHOD_ID", ErrorType.BUSINESS_RULE_ERROR, "PaymentMethodId value cannot be empty", null);
        }
        if (!value.startsWith("pm_")) {
            return Result.failure("INVALID_PAYMENT_METHOD_ID", ErrorType.BUSINESS_RULE_ERROR, "PaymentMethodId must start with 'pm_'", null);
        }
        return Result.success(new PaymentMethodId(value));
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

