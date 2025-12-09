package com.bcbs239.regtech.riskcalculation.domain.shared.enums;

/**
 * Economic sector categories for exposure classification
 * Used to categorize exposures by economic sector for concentration analysis
 */
public enum SectorCategory {
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
     * Interbank exposures
     */
    BANKING,
    
    /**
     * Other sectors not covered above
     */
    OTHER
}