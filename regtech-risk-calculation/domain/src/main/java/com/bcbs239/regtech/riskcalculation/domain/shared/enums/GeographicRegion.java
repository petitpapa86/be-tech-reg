package com.bcbs239.regtech.riskcalculation.domain.shared.enums;

/**
 * Geographic regions for exposure classification
 * Used to categorize exposures by geographic location for concentration analysis
 */
public enum GeographicRegion {
    /**
     * Home country exposures (Italy)
     */
    ITALY,
    
    /**
     * Other European Union countries (excluding Italy)
     */
    EU_OTHER,
    
    /**
     * Non-European countries (rest of world)
     */
    NON_EUROPEAN
}