package com.bcbs239.regtech.billing.domain.valueobjects;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
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
            return Result.failure(new ErrorDetail("INVALID_PAYMENT_METHOD_ID", "PaymentMethodId value cannot be null"));
        }
        if (value.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_PAYMENT_METHOD_ID", "PaymentMethodId value cannot be empty"));
        }
        if (!value.startsWith("pm_")) {
            return Result.failure(new ErrorDetail("INVALID_PAYMENT_METHOD_ID", "PaymentMethodId must start with 'pm_'"));
        }
        return Result.success(new PaymentMethodId(value));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
