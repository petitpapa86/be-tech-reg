package com.bcbs239.regtech.riskcalculation.presentation.exceptions;

import com.bcbs239.regtech.riskcalculation.domain.analysis.ProcessingState;

/**
 * Exception thrown when a calculation is requested but has not yet completed.
 * This exception should result in an HTTP 202 Accepted response, indicating
 * that the request is valid but processing is still in progress.
 * 
 * Requirements: 2.5, 7.1, 7.2
 */
public class CalculationNotCompletedException extends RuntimeException {
    
    private final String batchId;
    private final ProcessingState currentState;
    
    /**
     * Creates a new CalculationNotCompletedException.
     * 
     * @param batchId the batch identifier
     * @param currentState the current processing state
     */
    public CalculationNotCompletedException(String batchId, ProcessingState currentState) {
        super(String.format("Calculation for batch %s is not yet complete. Current state: %s", 
            batchId, currentState));
        this.batchId = batchId;
        this.currentState = currentState;
    }
    
    /**
     * Creates a new CalculationNotCompletedException with a custom message.
     * 
     * @param batchId the batch identifier
     * @param currentState the current processing state
     * @param message the custom error message
     */
    public CalculationNotCompletedException(String batchId, ProcessingState currentState, String message) {
        super(message);
        this.batchId = batchId;
        this.currentState = currentState;
    }
    
    /**
     * Gets the batch identifier.
     * 
     * @return the batch identifier
     */
    public String getBatchId() {
        return batchId;
    }
    
    /**
     * Gets the current processing state.
     * 
     * @return the current processing state
     */
    public ProcessingState getCurrentState() {
        return currentState;
    }
}
