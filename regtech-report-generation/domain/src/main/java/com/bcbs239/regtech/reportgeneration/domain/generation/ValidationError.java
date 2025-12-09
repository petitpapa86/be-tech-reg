package com.bcbs239.regtech.reportgeneration.domain.generation;

import lombok.NonNull;

import java.util.Optional;

/**
 * Validation Error value object
 * 
 * Represents a single validation error from XBRL schema validation,
 * including the error message and optional line number for debugging.
 */
public record ValidationError(
    @NonNull String message,
    Optional<Integer> lineNumber,
    Optional<String> columnNumber
) {
    
    /**
     * Compact constructor with validation
     */
    public ValidationError {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Error message cannot be null or blank");
        }
    }
    
    /**
     * Create a validation error with just a message
     */
    public static ValidationError of(String message) {
        return new ValidationError(message, Optional.empty(), Optional.empty());
    }
    
    /**
     * Create a validation error with message and line number
     */
    public static ValidationError of(String message, int lineNumber) {
        return new ValidationError(message, Optional.of(lineNumber), Optional.empty());
    }
    
    /**
     * Create a validation error with message, line number, and column number
     */
    public static ValidationError of(String message, int lineNumber, String columnNumber) {
        return new ValidationError(message, Optional.of(lineNumber), Optional.of(columnNumber));
    }
    
    /**
     * Check if the error has location information
     */
    public boolean hasLocation() {
        return lineNumber.isPresent();
    }
    
    /**
     * Get a formatted error message with location
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (lineNumber.isPresent()) {
            sb.append("Line ").append(lineNumber.get());
            if (columnNumber.isPresent()) {
                sb.append(", Column ").append(columnNumber.get());
            }
            sb.append(": ");
        }
        
        sb.append(message);
        
        return sb.toString();
    }
}
