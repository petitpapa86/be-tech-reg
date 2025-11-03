package com.bcbs239.regtech.dataquality.domain.validation;

import com.bcbs239.regtech.dataquality.domain.quality.DimensionScores;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Value object representing the complete validation results for a batch of exposures.
 * Contains both individual exposure results and batch-level validation information.
 */
public record ValidationResult(
    Map<String, ExposureValidationResult> exposureResults,
    List<ValidationError> batchErrors,
    List<ValidationError> allErrors,
    ValidationSummary summary,
    DimensionScores dimensionScores,
    int totalExposures,
    int validExposures
) {
    
    /**
     * Creates a builder for constructing ValidationResult instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a successful validation result (no errors)
     */
    public static ValidationResult success(int totalExposures) {
        return builder()
            .totalExposures(totalExposures)
            .validExposures(totalExposures)
            .exposureResults(Map.of())
            .batchErrors(List.of())
            .allErrors(List.of())
            .build();
    }
    
    /**
     * Gets validation result for a specific exposure
     */
    public ExposureValidationResult getExposureResult(String exposureId) {
        return exposureResults.get(exposureId);
    }
    
    /**
     * Checks if a specific exposure is valid
     */
    public boolean isExposureValid(String exposureId) {
        ExposureValidationResult result = getExposureResult(exposureId);
        return result != null && result.isValid();
    }
    
    /**
     * Gets all errors for a specific quality dimension
     */
    public List<ValidationError> getErrorsForDimension(QualityDimension dimension) {
        return allErrors.stream()
            .filter(error -> error.dimension() == dimension)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the count of errors for a specific dimension
     */
    public int getErrorCountForDimension(QualityDimension dimension) {
        return getErrorsForDimension(dimension).size();
    }
    
    /**
     * Gets all batch-level errors (not specific to any exposure)
     */
    public List<ValidationError> getBatchLevelErrors() {
        return batchErrors.stream()
            .filter(ValidationError::isBatchError)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets all exposure-level errors
     */
    public List<ValidationError> getExposureLevelErrors() {
        return allErrors.stream()
            .filter(ValidationError::isExposureError)
            .collect(Collectors.toList());
    }
    
    /**
     * Checks if the batch has any critical errors
     */
    public boolean hasCriticalErrors() {
        return allErrors.stream()
            .anyMatch(error -> error.severity() == ValidationError.ErrorSeverity.CRITICAL);
    }
    
    /**
     * Gets the overall validation rate (0.0 to 1.0)
     */
    public double getValidationRate() {
        return totalExposures > 0 ? (double) validExposures / totalExposures : 0.0;
    }
    
    /**
     * Gets the validation rate as a percentage (0-100)
     */
    public double getValidationRatePercentage() {
        return getValidationRate() * 100.0;
    }
    
    /**
     * Checks if the batch meets minimum quality standards
     */
    public boolean meetsQualityStandards(double minimumValidationRate) {
        return getValidationRate() >= minimumValidationRate && !hasCriticalErrors();
    }
    
    /**
     * Gets a summary of the most common error types
     */
    public Map<String, Long> getErrorCodeFrequency() {
        return allErrors.stream()
            .collect(Collectors.groupingBy(
                ValidationError::code,
                Collectors.counting()
            ));
    }
    
    /**
     * Builder class for ValidationResult
     */
    public static class Builder {
        private Map<String, ExposureValidationResult> exposureResults = Map.of();
        private List<ValidationError> batchErrors = List.of();
        private List<ValidationError> allErrors = List.of();
        private ValidationSummary summary;
        private DimensionScores dimensionScores;
        private int totalExposures;
        private int validExposures;
        
        public Builder exposureResults(Map<String, ExposureValidationResult> exposureResults) {
            this.exposureResults = exposureResults != null ? exposureResults : Map.of();
            return this;
        }
        
        public Builder batchErrors(List<ValidationError> batchErrors) {
            this.batchErrors = batchErrors != null ? batchErrors : List.of();
            return this;
        }
        
        public Builder allErrors(List<ValidationError> allErrors) {
            this.allErrors = allErrors != null ? allErrors : List.of();
            return this;
        }
        
        public Builder summary(ValidationSummary summary) {
            this.summary = summary;
            return this;
        }
        
        public Builder dimensionScores(DimensionScores dimensionScores) {
            this.dimensionScores = dimensionScores;
            return this;
        }
        
        public Builder totalExposures(int totalExposures) {
            this.totalExposures = totalExposures;
            return this;
        }
        
        public Builder validExposures(int validExposures) {
            this.validExposures = validExposures;
            return this;
        }
        
        public ValidationResult build() {
            // Auto-calculate valid exposures if not set
            if (validExposures == 0 && !exposureResults.isEmpty()) {
                validExposures = (int) exposureResults.values().stream()
                    .mapToLong(result -> result.isValid() ? 1 : 0)
                    .sum();
            }
            
            // Auto-calculate total exposures if not set
            if (totalExposures == 0 && !exposureResults.isEmpty()) {
                totalExposures = exposureResults.size();
            }
            
            // Auto-build summary if not provided
            if (summary == null) {
                summary = buildSummary();
            }
            
            return new ValidationResult(
                exposureResults,
                batchErrors,
                allErrors,
                summary,
                dimensionScores,
                totalExposures,
                validExposures
            );
        }
        
        private ValidationSummary buildSummary() {
            // Count errors by dimension
            Map<QualityDimension, Integer> errorsByDimension = allErrors.stream()
                .collect(Collectors.groupingBy(
                    ValidationError::dimension,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
            
            // Count errors by severity
            Map<ValidationError.ErrorSeverity, Integer> errorsBySeverity = allErrors.stream()
                .collect(Collectors.groupingBy(
                    ValidationError::severity,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
            
            // Count errors by code
            Map<String, Integer> errorsByCode = allErrors.stream()
                .collect(Collectors.groupingBy(
                    ValidationError::code,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
            
            return ValidationSummary.builder()
                .totalExposures(totalExposures)
                .validExposures(validExposures)
                .totalErrors(allErrors.size())
                .errorsByDimension(errorsByDimension)
                .errorsBySeverity(errorsBySeverity)
                .errorsByCode(errorsByCode)
                .build();
        }
    }
}