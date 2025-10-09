package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
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
        if (value.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_INVOICE_ID", "StripeInvoiceId value cannot be empty"));
        }
        if (!value.startsWith("in_")) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_INVOICE_ID", "StripeInvoiceId must start with 'in_'"));
        }
        return Result.success(new StripeInvoiceId(value));
    }
    
    @Override
    public String toString() {
        return value;
    }
}
