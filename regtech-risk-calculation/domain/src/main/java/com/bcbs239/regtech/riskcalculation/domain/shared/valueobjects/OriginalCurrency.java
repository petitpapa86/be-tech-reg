package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

/**
 * Original currency code before conversion to EUR
 * Immutable value object that preserves the original currency from exposure data
 */
public record OriginalCurrency(String code) {
    
    public OriginalCurrency {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        if (code.length() != 3) {
            throw new IllegalArgumentException("Currency code must be exactly 3 characters");
        }
        // Normalize to uppercase
        code = code.toUpperCase();
    }
    
    public static OriginalCurrency of(String code) {
        return new OriginalCurrency(code);
    }
    
    public boolean isEur() {
        return "EUR".equals(code);
    }
    
    public boolean isUsd() {
        return "USD".equals(code);
    }
    
    public boolean isGbp() {
        return "GBP".equals(code);
    }
}