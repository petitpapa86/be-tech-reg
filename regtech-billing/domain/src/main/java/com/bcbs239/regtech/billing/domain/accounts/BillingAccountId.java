package com.bcbs239.regtech.billing.domain.accounts;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * BillingAccountId value object representing a unique identifier for billing accounts.
 * Follows domain-driven design principles with validation and immutability.
 */
public record BillingAccountId(String value) {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");

    public BillingAccountId {
        Objects.requireNonNull(value, "BillingAccountId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("BillingAccountId value cannot be empty");
        }
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "BillingAccountId must be 3-50 characters long and contain only letters, numbers, hyphens, and underscores"
            );
        }
    }

    /**
     * Factory method to create BillingAccountId from string with validation
     */
    public static Result<BillingAccountId> fromString(String value) {
        try {
            return Result.success(new BillingAccountId(value));
        } catch (IllegalArgumentException e) {
            return Result.failure(new ErrorDetail("INVALID_BILLING_ACCOUNT_ID", e.getMessage()));
        }
    }

    /**
     * Generate a new BillingAccountId with a prefix
     */
    public static BillingAccountId generate(String prefix) {
        String id = prefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        return new BillingAccountId(id);
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Get the string value of this BillingAccountId
     */
    public String getValue() {
        return value;
    }
}