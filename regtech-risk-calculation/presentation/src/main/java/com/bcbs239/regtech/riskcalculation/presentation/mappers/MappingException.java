package com.bcbs239.regtech.riskcalculation.presentation.mappers;

/**
 * Exception thrown when mapping between domain objects and DTOs fails.
 * Provides descriptive error messages for debugging.
 * 
 * Requirements: 4.4
 */
public class MappingException extends RuntimeException {
    
    public MappingException(String message) {
        super(message);
    }
    
    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
