package com.bcbs239.regtech.billing.domain.invoices;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique identifier for an InvoiceLineItem.
 * Provides type safety and prevents mixing up different types of IDs.
 */
public record InvoiceLineItemId(String value) {
    
    public InvoiceLineItemId {
        Objects.requireNonNull(value, "InvoiceLineItemId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("InvoiceLineItemId value cannot be empty");
        }
    }
    
    /**
     * Generate a new unique InvoiceLineItemId.
     * 
     * @return A new InvoiceLineItemId with a UUID value
     */
    public static InvoiceLineItemId generate() {
        return new InvoiceLineItemId(UUID.randomUUID().toString());
    }
    
    /**
     * Create an InvoiceLineItemId from a string value.
     * 
     * @param value The string value to create the ID from
     * @return InvoiceLineItemId with the given value
     * @throws IllegalArgumentException if value is null or empty
     */
    public static InvoiceLineItemId fromString(String value) {
        return new InvoiceLineItemId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
