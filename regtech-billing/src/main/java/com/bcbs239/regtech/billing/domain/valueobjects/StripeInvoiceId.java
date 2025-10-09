package com.bcbs239.regtech.billing.domain.valueobjects;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.util.Objects;

/**
 * StripeInvoiceId value object representing Stripe's invoice identifier.
 * Wraps Stripe's invoice ID with validation.
 */
public record StripeInvoiceId(String value) {

    public StripeInvoiceId {
        Objects.requireNonNull(value, "StripeInvoiceId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("StripeInvoiceId value cannot be empty");
        }
        // Stripe invoice IDs start with "in_"
        if (!value.startsWith("in_")) {
            throw new IllegalArgumentException("StripeInvoiceId must start with 'in_'");
        }
    }

    /**
     * Factory method to create StripeInvoiceId from string with validation
     */
    public static Result<StripeInvoiceId> fromString(String value) {
        try {
            return Result.success(new StripeInvoiceId(value));
        } catch (IllegalArgumentException e) {
            return Result.failure(new ErrorDetail("INVALID_STRIPE_INVOICE_ID", e.getMessage()));
        }
    }

    /**
     * Create StripeInvoiceId from Stripe invoice object (placeholder for future Stripe integration)
     */
    public static StripeInvoiceId of(String stripeInvoiceId) {
        return new StripeInvoiceId(stripeInvoiceId);
    }

    @Override
    public String toString() {
        return value;
    }
}
