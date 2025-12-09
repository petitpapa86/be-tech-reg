package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Unique identifier for a bank
 * Immutable value object that ensures bank identification consistency
 */
public record BankId(String value) {
    
    public BankId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("BankId cannot be null or empty");
        }
    }
    
    public static BankId of(String value) {
        return new BankId(value);
    }
}