package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monetary amount in EUR currency
 * Immutable value object that ensures consistent currency handling
 */
public record AmountEur(BigDecimal value) {
    
    public AmountEur {
        if (value == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        // Ensure consistent scale (2 decimal places for EUR)
        value = value.setScale(2, RoundingMode.HALF_UP);
    }
    
    public static AmountEur of(BigDecimal value) {
        return new AmountEur(value);
    }
    
    public static AmountEur of(double value) {
        return new AmountEur(BigDecimal.valueOf(value));
    }
    
    public static AmountEur zero() {
        return new AmountEur(BigDecimal.ZERO);
    }
    
    public AmountEur add(AmountEur other) {
        return new AmountEur(this.value.add(other.value));
    }
    
    public AmountEur subtract(AmountEur other) {
        return new AmountEur(this.value.subtract(other.value));
    }
    
    public AmountEur multiply(BigDecimal multiplier) {
        return new AmountEur(this.value.multiply(multiplier));
    }
    
    public AmountEur divide(BigDecimal divisor) {
        return new AmountEur(this.value.divide(divisor, 2, RoundingMode.HALF_UP));
    }
    
    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isGreaterThan(AmountEur other) {
        return value.compareTo(other.value) > 0;
    }
}