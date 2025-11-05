package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

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
            return Result.failure(new ErrorDetail("INVALID_STRIPE_INVOICE_ID", "StripeInvoiceId value cannot be null"));
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_INVOICE_ID", "StripeInvoiceId value cannot be empty"));
        }
        if (normalized.equalsIgnoreCase("default")) {
            return Result.success(new StripeInvoiceId(normalized));
        }
        if (!normalized.startsWith("in_")) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_INVOICE_ID", "StripeInvoiceId must start with 'in_'"));
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

