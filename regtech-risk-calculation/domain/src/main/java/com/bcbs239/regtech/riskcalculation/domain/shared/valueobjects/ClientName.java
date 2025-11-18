package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Client name from exposure data
 * Immutable value object that represents the client/counterparty name
 */
public record ClientName(String value) {
    
    public ClientName {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Client name cannot be null or empty");
        }
        // Normalize whitespace
        value = value.trim();
    }
    
    public static ClientName of(String value) {
        return new ClientName(value);
    }
}