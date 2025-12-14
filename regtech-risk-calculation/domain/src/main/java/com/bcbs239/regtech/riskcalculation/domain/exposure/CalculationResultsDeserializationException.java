package com.bcbs239.regtech.riskcalculation.domain.exposure;

import lombok.Getter;

/**
 * Exception thrown when deserialization of JSON to calculation results fails.
 * This exception is part of the domain layer to support file-first architecture
 * where JSON files are the single source of truth for calculation results.
 * 
 * Requirement: 7.4 - Handle CalculationResultsDeserializationException for deserialization errors
 */
@Getter
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

}
