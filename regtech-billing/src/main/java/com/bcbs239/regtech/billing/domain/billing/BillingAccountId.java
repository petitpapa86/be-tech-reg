package com.bcbs239.regtech.billing.domain.billing;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for BillingAccount aggregate
 */
public record BillingAccountId(String value) {
    
    public BillingAccountId {
        Objects.requireNonNull(value, "BillingAccountId value cannot be null");
    }
    
    /**
     * Generate a new unique BillingAccountId
     */
    public static BillingAccountId generate() {
        return new BillingAccountId("billing-account-" + UUID.randomUUID().toString());
    }
    
    /**
     * Create BillingAccountId from string value with validation
     */
    public static Result<BillingAccountId> fromString(String value) {
        if (value == null) {
            return Result.failure(new ErrorDetail("INVALID_BILLING_ACCOUNT_ID", "BillingAccountId value cannot be null"));
        }
        if (value.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_BILLING_ACCOUNT_ID", "BillingAccountId value cannot be empty"));
        }
        return Result.success(new BillingAccountId(value));
    }
    
    @Override
    public String toString() {
        return value;
    }
}