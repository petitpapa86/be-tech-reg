package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Monetary amount in EUR currency
 * Immutable value object that ensures consistent currency handling
 * Used for representing financial amounts in reports
 */
public record AmountEur(BigDecimal value) implements Comparable<AmountEur> {
    
    /**
     * Compact constructor with validation
     */
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
    
    /**
     * Create an AmountEur from a BigDecimal value
     */
    public static AmountEur of(BigDecimal value) {
        return new AmountEur(value);
    }
    
    /**
     * Create an AmountEur from a double value
     */
    public static AmountEur of(double value) {
        return new AmountEur(BigDecimal.valueOf(value));
    }
    
    /**
     * Create an AmountEur from a long value
     */
    public static AmountEur of(long value) {
        return new AmountEur(BigDecimal.valueOf(value));
    }
    
    /**
     * Create a zero amount
     */
    public static AmountEur zero() {
        return new AmountEur(BigDecimal.ZERO);
    }
    
    /**
     * Add another amount to this amount
     */
    public AmountEur add(AmountEur other) {
        return new AmountEur(this.value.add(other.value));
    }
    
    /**
     * Subtract another amount from this amount
     */
    public AmountEur subtract(AmountEur other) {
        return new AmountEur(this.value.subtract(other.value));
    }
    
    /**
     * Multiply this amount by a multiplier
     */
    public AmountEur multiply(BigDecimal multiplier) {
        return new AmountEur(this.value.multiply(multiplier));
    }
    
    /**
     * Divide this amount by a divisor
     */
    public AmountEur divide(BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return new AmountEur(this.value.divide(divisor, 2, RoundingMode.HALF_UP));
    }
    
    /**
     * Check if this amount is zero
     */
    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Check if this amount is greater than another amount
     */
    public boolean isGreaterThan(AmountEur other) {
        return value.compareTo(other.value) > 0;
    }
    
    /**
     * Check if this amount is less than another amount
     */
    public boolean isLessThan(AmountEur other) {
        return value.compareTo(other.value) < 0;
    }
    
    /**
     * Check if this amount is greater than or equal to another amount
     */
    public boolean isGreaterThanOrEqual(AmountEur other) {
        return value.compareTo(other.value) >= 0;
    }
    
    /**
     * Check if this amount is less than or equal to another amount
     */
    public boolean isLessThanOrEqual(AmountEur other) {
        return value.compareTo(other.value) <= 0;
    }
    
    @Override
    public String toString() {
        return "€" + value.toPlainString();
    }

    @Override
    public int compareTo(AmountEur other) {
        return this.value.compareTo(other.value);
    }

    /**
     * Presentation-friendly formatted string (e.g., €1,234.56).
     * Intended for templates (Thymeleaf/SpringEL).
     */
    public String toFormattedString() {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        if (nf instanceof DecimalFormat df) {
            df.setRoundingMode(RoundingMode.HALF_UP);
        }
        return "€" + nf.format(value);
    }
}
