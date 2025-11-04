package com.bcbs239.regtech.billing.domain.invoicing;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.util.Objects;
import java.util.UUID;

/**
 * InvoiceId value object representing a unique identifier for invoices.
 * Uses UUID for global uniqueness across the system.
 */
public record InvoiceId(String value) {

    public InvoiceId {
        Objects.requireNonNull(value, "InvoiceId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("InvoiceId value cannot be empty");
        }
    }

    /**
     * Factory method to create InvoiceId from string with validation
     */
    public static Result<InvoiceId> fromString(String value) {
        try {
            // Validate that it's a valid UUID format
            UUID.fromString(value);
            return Result.success(new InvoiceId(value));
        } catch (IllegalArgumentException e) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE_ID",
                "InvoiceId must be a valid UUID format"));
        }
    }

    /**
     * Generate a new InvoiceId
     */
    public static InvoiceId generate() {
        return new InvoiceId(UUID.randomUUID().toString());
    }

    /**
     * Create InvoiceId with a specific UUID
     */
    public static InvoiceId of(UUID uuid) {
        return new InvoiceId(uuid.toString());
    }

    /**
     * Get the UUID representation
     */
    public UUID toUUID() {
        return UUID.fromString(value);
    }

    @Override
    public String toString() {
        return value;
    }
}