package com.bcbs239.regtech.riskcalculation.application.calculation;

/**
 * Exception thrown when deserialization of JSON to calculation results fails.
 * Requirement: 7.4 - Handle CalculationResultsDeserializationException for deserialization errors
 */
public class CalculationResultsDeserializationException extends RuntimeException {
    
    private final String jsonContent;
    
    public CalculationResultsDeserializationException(String message) {
        super(message);
        this.jsonContent = null;
    }
    
    public CalculationResultsDeserializationException(String message, Throwable cause) {
        super(message, cause);
        this.jsonContent = null;
    }
    
    public CalculationResultsDeserializationException(String message, String jsonContent, Throwable cause) {
        super(message, cause);
        this.jsonContent = jsonContent;
    }
    
    public String getJsonContent() {
        return jsonContent;
    }
}
