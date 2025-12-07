package com.bcbs239.regtech.riskcalculation.application.calculation;

/**
 * Exception thrown when serialization of calculation results to JSON fails.
 * Requirement: 7.1 - Handle CalculationResultsSerializationException for serialization errors
 */
public class CalculationResultsSerializationException extends RuntimeException {
    
    private final String batchId;
    
    public CalculationResultsSerializationException(String message, String batchId) {
        super(message);
        this.batchId = batchId;
    }
    
    public CalculationResultsSerializationException(String message, String batchId, Throwable cause) {
        super(message, cause);
        this.batchId = batchId;
    }
    
    public String getBatchId() {
        return batchId;
    }
}
