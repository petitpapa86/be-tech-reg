package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.riskcalculation.domain.shared.enums.ConcentrationLevel;
import com.bcbs239.regtech.riskcalculation.domain.aggregation.HerfindahlIndex;

/**
 * Concentration indices for risk assessment
 * Immutable value object containing Herfindahl-Hirschman indices for different dimensions
 */
public record ConcentrationIndices(
    HerfindahlIndex geographicHerfindahl,
    HerfindahlIndex sectorHerfindahl
) {
    
    public ConcentrationIndices {
        if (geographicHerfindahl == null) {
            throw new IllegalArgumentException("Geographic Herfindahl index cannot be null");
        }
        if (sectorHerfindahl == null) {
            throw new IllegalArgumentException("Sector Herfindahl index cannot be null");
        }
    }
    
    /**
     * Get the overall concentration level based on the highest HHI
     */
    public ConcentrationLevel getOverallConcentrationLevel() {
        ConcentrationLevel geoLevel = geographicHerfindahl.getConcentrationLevel();
        ConcentrationLevel sectorLevel = sectorHerfindahl.getConcentrationLevel();
        
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
    public HerfindahlIndex getMaximumHerfindahl() {
        return geographicHerfindahl.value().compareTo(sectorHerfindahl.value()) >= 0 
            ? geographicHerfindahl 
            : sectorHerfindahl;
    }
}