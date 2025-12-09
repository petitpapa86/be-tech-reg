package com.bcbs239.regtech.riskcalculation.domain.calculation;

/**
 * Status of a batch calculation
 * Represents the lifecycle state of a batch being processed
 */
public enum BatchStatus {
    /**
     * Batch is currently being processed
     */
    PROCESSING,
    
    /**
     * Batch processing completed successfully
     */
    COMPLETED,
    
    /**
     * Batch processing failed
     */
    FAILED
}
