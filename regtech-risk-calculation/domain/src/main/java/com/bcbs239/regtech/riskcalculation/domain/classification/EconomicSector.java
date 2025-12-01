package com.bcbs239.regtech.riskcalculation.domain.classification;

/**
 * Economic sector categories for exposure classification
 * Used to categorize exposures by economic sector for concentration analysis
 * Part of the Classification Service bounded context
 */
public enum EconomicSector {
    /**
     * Retail mortgage loans
     */
    RETAIL_MORTGAGE,
    
    /**
     * Government bonds and sovereign exposures
     */
    SOVEREIGN,
    
    /**
     * Corporate loans and bonds
     */
    CORPORATE,
    
    /**
     * Interbank exposures and banking sector
     */
    BANKING,
    
    /**
     * Other sectors not covered above
     */
    OTHER
}
