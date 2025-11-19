package com.bcbs239.regtech.reportgeneration.domain.generation;

import lombok.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Validation Result value object
 * 
 * Contains the result of XBRL validation against EBA XSD schema,
 * including validation status and any error details.
 */
public record ValidationResult(
    boolean valid,
    @NonNull List<ValidationError> errors
) {
    
    /**
     * Compact constructor with validation
     */
    public ValidationResult {
        if (errors == null) {
            throw new IllegalArgumentException("Errors list cannot be null");
        }
        // Make the list immutable
        errors = Collections.unmodifiableList(errors);
    }
    
    /**
     * Create a successful validation result
     */
    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }
    
    /**
     * Create a failed validation result with errors
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("Failure result must have at least one error");
        }
        return new ValidationResult(false, errors);
    }
    
    /**
     * Check if validation failed
     */
    public boolean isInvalid() {
        return !valid;
    }
    
    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Get the number of validation errors
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * Get a formatted error message with all errors
     */
    public String getFormattedErrorMessage() {
        if (valid) {
            return "Validation successful";
        }
        
        StringBuilder sb = new StringBuilder("Validation failed with ")
            .append(errors.size())
            .append(" error(s):\n");
        
        for (int i = 0; i < errors.size(); i++) {
            ValidationError error = errors.get(i);
            sb.append(i + 1)
              .append(". ")
              .append(error.toString())
              .append("\n");
        }
        
        return sb.toString();
    }
}
