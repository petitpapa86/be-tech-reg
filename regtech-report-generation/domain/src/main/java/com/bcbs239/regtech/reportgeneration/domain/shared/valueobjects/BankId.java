package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import lombok.NonNull;

/**
 * Unique identifier for a bank
 * Immutable value object that ensures bank identification consistency
 */
public record BankId(@NonNull String value) {
    
    public BankId {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("BankId cannot be empty");
        }
    }
    
    public static BankId of(String value) {
        return new BankId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
