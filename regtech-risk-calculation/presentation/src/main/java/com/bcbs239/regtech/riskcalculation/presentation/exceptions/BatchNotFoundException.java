package com.bcbs239.regtech.riskcalculation.presentation.exceptions;

/**
 * Exception thrown when a requested batch cannot be found.
 * This exception should result in an HTTP 404 Not Found response.
 * 
 * Requirements: 2.5, 7.1, 7.2
 */
public class BatchNotFoundException extends RuntimeException {
    
    private final String batchId;
    
    /**
     * Creates a new BatchNotFoundException.
     * 
     * @param batchId the batch identifier that was not found
     */
    public BatchNotFoundException(String batchId) {
        super(String.format("Batch not found: %s", batchId));
        this.batchId = batchId;
    }
    
    /**
     * Creates a new BatchNotFoundException with a custom message.
     * 
     * @param batchId the batch identifier that was not found
     * @param message the custom error message
     */
    public BatchNotFoundException(String batchId, String message) {
        super(message);
        this.batchId = batchId;
    }
    
    /**
     * Gets the batch identifier that was not found.
     * 
     * @return the batch identifier
     */
    public String getBatchId() {
        return batchId;
    }
}
