package com.bcbs239.regtech.modules.dataquality.domain.validation;

import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityDimension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Value object representing validation results for a single exposure record.
 * Contains dimension-specific errors and overall validation status.
 */
public record ExposureValidationResult(
    String exposureId,
    List<ValidationError> errors,
    Map<QualityDimension, List<ValidationError>> dimensionErrors,
    boolean isValid
) {
    
    /**
     * Creates a builder for constructing ExposureValidationResult instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a successful validation result (no errors)
     */
    public static ExposureValidationResult success(String exposureId) {
        return new ExposureValidationResult(
            exposureId,
            List.of(),
            Map.of(),
            true
        );
    }
    
    /**
     * Creates a failed validation result with errors
     */
    public static ExposureValidationResult failure(String exposureId, List<ValidationError> errors) {
        Map<QualityDimension, List<ValidationError>> dimensionErrors = errors.stream()
            .collect(Collectors.groupingBy(ValidationError::dimension));
        
        return new ExposureValidationResult(
            exposureId,
            errors,
            dimensionErrors,
            false
        );
    }
    
    /**
     * Gets errors for a specific quality dimension
     */
    public List<ValidationError> getErrorsForDimension(QualityDimension dimension) {
        return dimensionErrors.getOrDefault(dimension, List.of());
    }
    
    /**
     * Checks if a specific dimension has errors
     */
    public boolean hasDimensionErrors(QualityDimension dimension) {
        return dimensionErrors.containsKey(dimension) && 
               !dimensionErrors.get(dimension).isEmpty();
    }
    
    /**
     * Gets the count of errors for a specific dimension
     */
    public int getErrorCountForDimension(QualityDimension dimension) {
        return getErrorsForDimension(dimension).size();
    }
    
    /**
     * Gets the total number of validation errors
     */
    public int getTotalErrorCount() {
        return errors.size();
    }
    
    /**
     * Gets errors by severity level
     */
    public List<ValidationError> getErrorsBySeverity(ValidationError.ErrorSeverity severity) {
        return errors.stream()
            .filter(error -> error.severity() == severity)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the count of critical errors
     */
    public int getCriticalErrorCount() {
        return getErrorsBySeverity(ValidationError.ErrorSeverity.CRITICAL).size();
    }
    
    /**
     * Checks if this exposure has critical errors
     */
    public boolean hasCriticalErrors() {
        return getCriticalErrorCount() > 0;
    }
    
    /**
     * Gets a summary of errors by dimension
     */
    public Map<QualityDimension, Integer> getErrorCountsByDimension() {
        return dimensionErrors.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().size()
            ));
    }
    
    /**
     * Builder class for ExposureValidationResult
     */
    public static class Builder {
        private String exposureId;
        private List<ValidationError> errors = List.of();
        private Map<QualityDimension, List<ValidationError>> dimensionErrors = Map.of();
        private boolean isValid = true;
        
        public Builder exposureId(String exposureId) {
            this.exposureId = exposureId;
            return this;
        }
        
        public Builder errors(List<ValidationError> errors) {
            this.errors = errors != null ? errors : List.of();
            this.isValid = this.errors.isEmpty();
            return this;
        }
        
        public Builder dimensionErrors(Map<QualityDimension, List<ValidationError>> dimensionErrors) {
            this.dimensionErrors = dimensionErrors != null ? dimensionErrors : Map.of();
            return this;
        }
        
        public Builder isValid(boolean isValid) {
            this.isValid = isValid;
            return this;
        }
        
        public ExposureValidationResult build() {
            // Auto-calculate dimension errors if not provided
            if (dimensionErrors.isEmpty() && !errors.isEmpty()) {
                dimensionErrors = errors.stream()
                    .collect(Collectors.groupingBy(ValidationError::dimension));
            }
            
            return new ExposureValidationResult(
                exposureId,
                errors,
                dimensionErrors,
                isValid
            );
        }
    }
}