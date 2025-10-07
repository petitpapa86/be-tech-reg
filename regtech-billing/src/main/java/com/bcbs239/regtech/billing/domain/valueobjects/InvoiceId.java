package com.bcbs239.regtech.billing.domain.valueobjects;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
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
            return Result.failure(new ErrorDetail("INVALID_INVOICE_ID", "InvoiceId value cannot be null"));
        }
        if (value.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE_ID", "InvoiceId value cannot be empty"));
        }
        return Result.success(new InvoiceId(value));
    }
    
    @Override
    public String toString() {
        return value;
    }
}