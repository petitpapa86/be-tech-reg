package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Total monetary amount in EUR for a portfolio or breakdown
 * Immutable value object that represents aggregated amounts
 */
public record TotalAmountEur(BigDecimal value) {
    
    public TotalAmountEur {
        if (value == null) {
            throw new IllegalArgumentException("Total amount cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }
        // Ensure consistent scale (2 decimal places for EUR)
        value = value.setScale(2, RoundingMode.HALF_UP);
    }
    
    public static TotalAmountEur of(BigDecimal value) {
        return new TotalAmountEur(value);
    }
    
    public static TotalAmountEur of(double value) {
        return new TotalAmountEur(BigDecimal.valueOf(value));
    }
    
    public static TotalAmountEur zero() {
        return new TotalAmountEur(BigDecimal.ZERO);
    }
    
    public TotalAmountEur add(AmountEur amount) {
        return new TotalAmountEur(this.value.add(amount.value()));
    }
    
    public TotalAmountEur add(TotalAmountEur other) {
        return new TotalAmountEur(this.value.add(other.value));
    }
    
    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isGreaterThan(TotalAmountEur other) {
        return value.compareTo(other.value) > 0;
    }
}