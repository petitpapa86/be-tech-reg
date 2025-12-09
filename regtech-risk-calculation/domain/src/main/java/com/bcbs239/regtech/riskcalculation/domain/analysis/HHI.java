package com.bcbs239.regtech.riskcalculation.domain.analysis;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.ConcentrationLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing the Herfindahl-Hirschman Index (HHI)
 * HHI is calculated as the sum of squared market shares
 * Part of the Portfolio Analysis bounded context
 */
public record HHI(BigDecimal value, ConcentrationLevel level) {
    
    public HHI {
        Objects.requireNonNull(value, "HHI value cannot be null");
        Objects.requireNonNull(level, "Concentration level cannot be null");
        
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("HHI must be between 0 and 1");
        }
    }
    
    /**
     * Calculate HHI from a breakdown of shares
     * HHI = Σ(share_i)² where shares are expressed as decimals (not percentages)
     * 
     * @param breakdown the breakdown containing shares
     * @return HHI with calculated value and concentration level
     */
    public static HHI calculate(Breakdown breakdown) {
        Objects.requireNonNull(breakdown, "Breakdown cannot be null");
        
        BigDecimal hhi = breakdown.shares().values().stream()
            .map(share -> {
                BigDecimal decimal = share.getDecimalShare();
                return decimal.multiply(decimal);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(4, RoundingMode.HALF_UP);
        
        ConcentrationLevel level = determineConcentrationLevel(hhi);
        
        return new HHI(hhi, level);
    }
    
    /**
     * Determine concentration level based on HHI value
     * - LOW: HHI < 0.15
     * - MODERATE: 0.15 <= HHI < 0.25
     * - HIGH: HHI >= 0.25
     */
    private static ConcentrationLevel determineConcentrationLevel(BigDecimal hhi) {
        if (hhi.compareTo(BigDecimal.valueOf(0.15)) < 0) {
            return ConcentrationLevel.LOW;
        } else if (hhi.compareTo(BigDecimal.valueOf(0.25)) < 0) {
            return ConcentrationLevel.MODERATE;
        } else {
            return ConcentrationLevel.HIGH;
        }
    }
}
