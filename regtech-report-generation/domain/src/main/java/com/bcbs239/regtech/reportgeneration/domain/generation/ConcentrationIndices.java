package com.bcbs239.regtech.reportgeneration.domain.generation;

import lombok.NonNull;

import java.math.BigDecimal;

/**
 * Concentration indices for risk assessment
 * Immutable value object containing Herfindahl-Hirschman indices for different dimensions
 * 
 * <p><strong>Aggregate-Specific Value Object:</strong> This value object is exclusively used by the
 * {@link GeneratedReport} aggregate to represent concentration risk metrics specific to report
 * generation. It encapsulates the Herfindahl-Hirschman Index (HHI) calculations for geographic
 * and sector dimensions. Following DDD principles, it is co-located with its aggregate in the
 * generation package to maintain high cohesion.</p>
 * 
 * @see GeneratedReport
 * @see com.bcbs239.regtech.reportgeneration.domain.generation
 */
public record ConcentrationIndices(
    @NonNull BigDecimal geographicHerfindahl,
    @NonNull BigDecimal sectorHerfindahl
) {
    
    /**
     * Compact constructor with validation
     */
    public ConcentrationIndices {
        if (geographicHerfindahl.compareTo(BigDecimal.ZERO) < 0 || geographicHerfindahl.compareTo(new BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("Geographic Herfindahl index must be between 0 and 10000");
        }
        if (sectorHerfindahl.compareTo(BigDecimal.ZERO) < 0 || sectorHerfindahl.compareTo(new BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("Sector Herfindahl index must be between 0 and 10000");
        }
    }
    
    /**
     * Get the overall concentration level based on the highest HHI
     * HHI < 1500: Low concentration
     * 1500 <= HHI < 2500: Moderate concentration
     * HHI >= 2500: High concentration
     */
    public ConcentrationLevel getOverallConcentrationLevel() {
        ConcentrationLevel geoLevel = getConcentrationLevel(geographicHerfindahl);
        ConcentrationLevel sectorLevel = getConcentrationLevel(sectorHerfindahl);
        
        // Return the higher concentration level
        if (geoLevel == ConcentrationLevel.HIGH || sectorLevel == ConcentrationLevel.HIGH) {
            return ConcentrationLevel.HIGH;
        }
        if (geoLevel == ConcentrationLevel.MODERATE || sectorLevel == ConcentrationLevel.MODERATE) {
            return ConcentrationLevel.MODERATE;
        }
        return ConcentrationLevel.LOW;
    }
    
    /**
     * Check if any concentration index indicates high risk
     */
    public boolean hasHighConcentrationRisk() {
        return getOverallConcentrationLevel() == ConcentrationLevel.HIGH;
    }
    
    /**
     * Get the maximum HHI value across all dimensions
     */
    public BigDecimal getMaximumHerfindahl() {
        return geographicHerfindahl.compareTo(sectorHerfindahl) >= 0 
            ? geographicHerfindahl 
            : sectorHerfindahl;
    }
    
    /**
     * Get concentration level for a given HHI value
     */
    private ConcentrationLevel getConcentrationLevel(BigDecimal hhi) {
        if (hhi.compareTo(new BigDecimal("1500")) < 0) {
            return ConcentrationLevel.LOW;
        } else if (hhi.compareTo(new BigDecimal("2500")) < 0) {
            return ConcentrationLevel.MODERATE;
        } else {
            return ConcentrationLevel.HIGH;
        }
    }
    
    /**
     * Concentration level enum
     */
    public enum ConcentrationLevel {
        LOW,
        MODERATE,
        HIGH
    }
}
