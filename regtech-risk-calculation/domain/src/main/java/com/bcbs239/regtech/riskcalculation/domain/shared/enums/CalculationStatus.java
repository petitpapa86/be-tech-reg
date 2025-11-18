package com.bcbs239.regtech.riskcalculation.domain.shared.enums;

/**
 * Status of risk calculation process
 * Tracks the lifecycle of a batch calculation from initiation to completion
 */
public enum CalculationStatus {
    /**
     * Calculation has been initiated but not yet started
     */
    PENDING,
    
    /**
     * Currently downloading exposure data from storage
     */
    DOWNLOADING,
    
    /**
     * Currently processing exposures and calculating metrics
     */
    CALCULATING,
    
    /**
     * Calculation completed successfully
     */
    COMPLETED,
    
    /**
     * Calculation failed due to an error
     */
    FAILED
}