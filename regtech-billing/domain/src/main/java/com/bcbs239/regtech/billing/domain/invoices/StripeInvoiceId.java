package com.bcbs239.regtech.billing.domain.invoices;



import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.Objects;

/**
 * Typed wrapper for Stripe Invoice ID
 */
public record StripeInvoiceId(String value) {
    
    public StripeInvoiceId {
        Objects.requireNonNull(value, "StripeInvoiceId value cannot be null");
    }
    
    /**
     * Create StripeInvoiceId from string value with validation
     */
    public static Result<StripeInvoiceId> fromString(String value) {
        if (value == null) {
            return Result.failure("INVALID_STRIPE_INVOICE_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeInvoiceId value cannot be null", null);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return Result.failure("INVALID_STRIPE_INVOICE_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeInvoiceId value cannot be empty", null);
        }
        if (normalized.equalsIgnoreCase("default")) {
            return Result.success(new StripeInvoiceId(normalized));
        }
        if (!normalized.startsWith("in_")) {
            return Result.failure("INVALID_STRIPE_INVOICE_ID", ErrorType.BUSINESS_RULE_ERROR, "StripeInvoiceId must start with 'in_'", null);
        }
        return Result.success(new StripeInvoiceId(normalized));
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

