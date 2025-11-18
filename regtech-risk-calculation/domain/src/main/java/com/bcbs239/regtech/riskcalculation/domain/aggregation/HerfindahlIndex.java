package com.bcbs239.regtech.riskcalculation.domain.aggregation;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.ConcentrationLevel;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalAmountEur;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Herfindahl-Hirschman Index for measuring concentration
 * Immutable value object that calculates and represents concentration risk
 */
public record HerfindahlIndex(BigDecimal value) {
    
    public HerfindahlIndex {
        if (value == null) {
            throw new IllegalArgumentException("Herfindahl index value cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Herfindahl index cannot be negative");
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Herfindahl index cannot exceed 1.0");
        }
        // Ensure consistent scale (4 decimal places for HHI)
        value = value.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate Herfindahl-Hirschman Index from a breakdown of amounts
     * Formula: HHI = Σ(share_i)²
     */
    public static HerfindahlIndex calculate(Map<?, TotalAmountEur> breakdown, TotalAmountEur total) {
        if (breakdown == null || breakdown.isEmpty()) {
            return new HerfindahlIndex(BigDecimal.ZERO);
        }
        
        if (total == null || total.isZero()) {
            return new HerfindahlIndex(BigDecimal.ZERO);
        }
        
        double hhi = breakdown.values().stream()
            .mapToDouble(amount -> {
                // Calculate share as a decimal (not percentage)
                double share = amount.value().divide(total.value(), 6, RoundingMode.HALF_UP).doubleValue();
                return share * share; // Square the share
            })
            .sum();
        
        return new HerfindahlIndex(BigDecimal.valueOf(hhi).setScale(4, RoundingMode.HALF_UP));
    }
    
    /**
     * Calculate HHI from percentage shares (already in decimal form)
     */
    public static HerfindahlIndex calculateFromShares(Map<?, BigDecimal> shares) {
        if (shares == null || shares.isEmpty()) {
            return new HerfindahlIndex(BigDecimal.ZERO);
        }
        
        double hhi = shares.values().stream()
            .mapToDouble(share -> {
                double shareValue = share.doubleValue();
                return shareValue * shareValue;
            })
            .sum();
        
        return new HerfindahlIndex(BigDecimal.valueOf(hhi).setScale(4, RoundingMode.HALF_UP));
    }
    
    /**
     * Get the concentration level based on HHI thresholds
     * - HHI < 0.15: Low concentration
     * - 0.15 ≤ HHI < 0.25: Moderate concentration  
     * - HHI ≥ 0.25: High concentration
     */
    public ConcentrationLevel getConcentrationLevel() {
        if (value.compareTo(new BigDecimal("0.15")) < 0) {
            return ConcentrationLevel.LOW;
        }
        if (value.compareTo(new BigDecimal("0.25")) < 0) {
            return ConcentrationLevel.MODERATE;
        }
        return ConcentrationLevel.HIGH;
    }
    
    /**
     * Check if this represents high concentration risk
     */
    public boolean isHighConcentration() {
        return getConcentrationLevel() == ConcentrationLevel.HIGH;
    }
    
    /**
     * Check if this represents moderate concentration risk
     */
    public boolean isModerateConcentration() {
        return getConcentrationLevel() == ConcentrationLevel.MODERATE;
    }
    
    /**
     * Check if this represents low concentration risk
     */
    public boolean isLowConcentration() {
        return getConcentrationLevel() == ConcentrationLevel.LOW;
    }
    
    /**
     * Compare with another HHI
     */
    public boolean isGreaterThan(HerfindahlIndex other) {
        return value.compareTo(other.value) > 0;
    }
    
    /**
     * Get HHI as percentage (multiply by 100)
     */
    public BigDecimal asPercentage() {
        return value.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
}