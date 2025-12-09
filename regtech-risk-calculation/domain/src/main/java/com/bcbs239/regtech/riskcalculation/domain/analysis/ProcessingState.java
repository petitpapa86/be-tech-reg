package com.bcbs239.regtech.riskcalculation.domain.analysis;

/**
 * Represents the current state of portfolio analysis processing.
 * Used to track progress and enable resumability of large batch processing operations.
 */
public enum ProcessingState {
    
    /**
     * Analysis has been created but processing has not started.
     */
    PENDING,
    
    /**
     * Analysis is currently being processed.
     * Processing can be resumed from the last completed chunk if interrupted.
     */
    IN_PROGRESS,
    
    /**
     * Analysis processing has completed successfully.
     */
    COMPLETED,
    
    /**
     * Analysis processing has failed and cannot continue.
     * Manual intervention may be required.
     */
    FAILED
}
