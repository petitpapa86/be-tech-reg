package com.bcbs239.regtech.riskcalculation.domain.storage;

import lombok.Getter;

/**
 * Exception thrown when attempting to overwrite existing calculation results.
 * This exception enforces the immutability requirement where JSON files serve
 * as write-once audit records that cannot be modified or overwritten.
 * 
 * Requirement: 8.1 - Ensure JSON files are immutable (write-once)
 * Requirement: 8.4 - Do not modify or overwrite existing JSON files
 */
@Getter
public class CalculationResultsImmutabilityException extends RuntimeException {
    
    private final String batchId;
    private final String existingUri;
    
    public CalculationResultsImmutabilityException(String message, String batchId, String existingUri) {
        super(message);
        this.batchId = batchId;
        this.existingUri = existingUri;
    }
    
    public CalculationResultsImmutabilityException(String message, String batchId, String existingUri, Throwable cause) {
        super(message, cause);
        this.batchId = batchId;
        this.existingUri = existingUri;
    }

}
