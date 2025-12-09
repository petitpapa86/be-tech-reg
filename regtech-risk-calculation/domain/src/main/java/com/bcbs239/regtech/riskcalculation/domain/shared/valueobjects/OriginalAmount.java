package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import java.math.BigDecimal;

/**
 * Original monetary amount before currency conversion
 * Immutable value object that preserves the original amount from exposure data
 */
public record OriginalAmount(BigDecimal value) {
    
    public OriginalAmount {
        if (value == null) {
            throw new IllegalArgumentException("Original amount cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Original amount cannot be negative");
        }
    }
    
    public static OriginalAmount of(BigDecimal value) {
        return new OriginalAmount(value);
    }
    
    public static OriginalAmount of(double value) {
        return new OriginalAmount(BigDecimal.valueOf(value));
    }
    
    public static OriginalAmount zero() {
        return new OriginalAmount(BigDecimal.ZERO);
    }
    
    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isGreaterThan(OriginalAmount other) {
        return value.compareTo(other.value) > 0;
    }
}