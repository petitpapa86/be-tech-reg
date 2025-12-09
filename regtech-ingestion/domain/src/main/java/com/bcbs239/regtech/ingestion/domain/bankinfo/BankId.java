package com.bcbs239.regtech.ingestion.domain.bankinfo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * Value object representing a bank identifier.
 */
public record BankId(@JsonValue String value) {
    
    @JsonCreator
    public BankId {
        Objects.requireNonNull(value, "BankId value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("BankId value cannot be empty");
        }
        if (value.length() > 20) {
            throw new IllegalArgumentException("BankId value cannot exceed 20 characters");
        }
    }
    
    /**
     * Create a BankId from a string value.
     */
    public static BankId of(String value) {
        return new BankId(value);
    }

}

