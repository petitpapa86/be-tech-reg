package com.bcbs239.regtech.riskcalculation.domain.exposure;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Monetary amount in a specific currency
 * Represents the original exposure amount before conversion
 * Immutable value object
 */
public record MonetaryAmount(
    BigDecimal amount,
    String currencyCode
) {
    
    public MonetaryAmount {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currencyCode, "Currency code cannot be null");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be empty");
        }
        if (currencyCode.length() != 3) {
            throw new IllegalArgumentException("Currency code must be 3 characters (ISO 4217)");
        }
    }
    
    public static MonetaryAmount of(BigDecimal amount, String currencyCode) {
        return new MonetaryAmount(amount, currencyCode.toUpperCase());
    }
    
    public static MonetaryAmount zero(String currencyCode) {
        return new MonetaryAmount(BigDecimal.ZERO, currencyCode.toUpperCase());
    }
}
