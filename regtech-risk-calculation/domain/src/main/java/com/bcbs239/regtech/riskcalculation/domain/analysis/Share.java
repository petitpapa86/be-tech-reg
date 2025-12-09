package com.bcbs239.regtech.riskcalculation.domain.analysis;

import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a share of the total portfolio
 * Contains both the absolute amount and the percentage of total
 * Part of the Portfolio Analysis bounded context
 */
public record Share(
    EurAmount amount,
    BigDecimal percentage
) {
    
    public Share {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(percentage, "Percentage cannot be null");
        
        if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Percentage cannot be negative");
        }
        if (percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage cannot exceed 100%");
        }
    }
    
    /**
     * Calculate share from an amount and total
     * 
     * @param amount the amount for this share
     * @param total the total portfolio amount
     * @return Share with calculated percentage
     */
    public static Share calculate(BigDecimal amount, BigDecimal total) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(total, "Total cannot be null");
        
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return new Share(EurAmount.zero(), BigDecimal.ZERO);
        }
        
        BigDecimal pct = amount
            .divide(total, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        return new Share(EurAmount.of(amount), pct);
    }
    
    /**
     * Get the percentage as a decimal (0.0 to 1.0) for HHI calculation
     */
    public BigDecimal getDecimalShare() {
        return percentage.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }
}
