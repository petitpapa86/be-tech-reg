package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Percentage of total portfolio for an exposure or breakdown
 * Immutable value object that represents percentage values with proper precision
 */
public record PercentageOfTotal(BigDecimal value) {
    
    public PercentageOfTotal {
        if (value == null) {
            throw new IllegalArgumentException("Percentage cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage cannot be negative");
        }
        if (value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage cannot exceed 100%");
        }
        // Ensure consistent scale (2 decimal places for percentages)
        value = value.setScale(2, RoundingMode.HALF_UP);
    }
    
    public static PercentageOfTotal of(BigDecimal value) {
        return new PercentageOfTotal(value);
    }
    
    public static PercentageOfTotal of(double value) {
        return new PercentageOfTotal(BigDecimal.valueOf(value));
    }
    
    public static PercentageOfTotal zero() {
        return new PercentageOfTotal(BigDecimal.ZERO);
    }
    
    public static PercentageOfTotal calculate(AmountEur amount, TotalAmountEur total) {
        if (total.isZero()) {
            return zero();
        }
        BigDecimal percentage = amount.value()
            .divide(total.value(), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
        return new PercentageOfTotal(percentage);
    }
    
    public static PercentageOfTotal calculate(TotalAmountEur amount, TotalAmountEur total) {
        if (total.isZero()) {
            return zero();
        }
        BigDecimal percentage = amount.value()
            .divide(total.value(), 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
        return new PercentageOfTotal(percentage);
    }
    
    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isGreaterThan(PercentageOfTotal other) {
        return value.compareTo(other.value) > 0;
    }
}