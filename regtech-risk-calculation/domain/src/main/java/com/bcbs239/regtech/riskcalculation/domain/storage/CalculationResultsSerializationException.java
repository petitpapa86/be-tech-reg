package com.bcbs239.regtech.riskcalculation.domain.storage;

/**
 * Exception thrown when serialization of calculation results to JSON fails.
 * This exception is part of the domain layer to support file-first architecture
 * where JSON files are the single source of truth for calculation results.
 * 
 * Requirement: 7.1 - Handle CalculationResultsSerializationException for serialization errors
 */
public class CalculationResultsSerializationException extends RuntimeException {
    
    private final String batchId;
    
    public CalculationResultsSerializationException(String message) {
        super(message);
        this.batchId = null;
    }
    
    public CalculationResultsSerializationException(String message, Throwable cause) {
        super(message, cause);
        this.batchId = null;
    }
    
    public CalculationResultsSerializationException(String message, String batchId, Throwable cause) {
        super(message, cause);
        this.batchId = batchId;
    }
    
    public String getBatchId() {
        return batchId;
    }
}
