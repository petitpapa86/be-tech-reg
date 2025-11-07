package com.bcbs239.regtech.billing.domain.invoices;


import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for Invoice aggregate
 */
public record InvoiceId(String value) {
    
    public InvoiceId {
        Objects.requireNonNull(value, "InvoiceId value cannot be null");
    }
    
    /**
     * Generate a new unique InvoiceId
     */
    public static InvoiceId generate() {
        return new InvoiceId("invoice-" + UUID.randomUUID().toString());
    }
    
    /**
     * Create InvoiceId from string value with validation
     */
    public static Result<InvoiceId> fromString(String value) {
        if (value == null) {
            return Result.failure("INVALID_INVOICE_ID", ErrorType.BUSINESS_RULE_ERROR, "InvoiceId value cannot be null", null);
        }
        if (value.trim().isEmpty()) {
            return Result.failure("INVALID_INVOICE_ID", ErrorType.BUSINESS_RULE_ERROR, "InvoiceId value cannot be empty", null);
        }
        return Result.success(new InvoiceId(value));
    }

    /**
     * Get the string value of this InvoiceId
     */
    public String getValue() {
        return value;
    }
}

