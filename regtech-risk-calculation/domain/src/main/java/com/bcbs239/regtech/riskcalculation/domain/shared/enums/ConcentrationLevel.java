package com.bcbs239.regtech.riskcalculation.domain.shared.enums;

/**
 * Concentration risk levels based on Herfindahl-Hirschman Index values
 * Used to categorize portfolio concentration risk
 */
public enum ConcentrationLevel {
    /**
     * Low concentration (HHI < 0.15)
     */
    LOW,
    
    /**
     * Moderate concentration (0.15 <= HHI < 0.25)
     */
    MODERATE,
    
    /**
     * High concentration (HHI >= 0.25)
     */
    HIGH
}