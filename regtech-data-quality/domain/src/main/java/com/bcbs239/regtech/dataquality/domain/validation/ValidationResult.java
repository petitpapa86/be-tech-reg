package com.bcbs239.regtech.dataquality.domain.validation;

import com.bcbs239.regtech.dataquality.domain.quality.DimensionScores;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Value object representing the complete validation results for a batch of exposures.
 * Contains both individual exposure results and batch-level validation information.
 * 
 * <p>This value object now uses the Rules Engine exclusively for validation,
 * replacing the previous Specification-based approach. The Rules Engine provides
 * database-driven, configurable validation rules that can be modified without code deployment.</p>
 * 
 * <p>This is proper DDD - the value object encapsulates the logic for its own creation
 * by delegating to the Rules Engine through the DataQualityRulesService.</p>
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
     * Creates an empty validation result (for zero exposures)
     */
    public static ValidationResult empty() {
        return builder()
            .totalExposures(0)
            .validExposures(0)
            .exposureResults(Map.of())
            .batchErrors(List.of())
            .allErrors(List.of())
            .dimensionScores(new DimensionScores(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))
            .build();
    }
    
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
     * Factory method to create a ValidationResult from pre-validated exposure results.
     * 
     * <p>This follows proper DDD - the value object is created from data, not by orchestrating
     * infrastructure services. The application layer is responsible for calling the Rules Engine
     * and passing the results here.</p>
     * 
     * @param exposureResults Map of exposure IDs to their validation results
     * @param batchErrors List of batch-level validation errors
     * @return ValidationResult containing all validation errors and dimension scores
     */
    public static ValidationResult fromValidatedExposures(
            Map<String, ExposureValidationResult> exposureResults,
            List<ValidationError> batchErrors) {
        
        Objects.requireNonNull(exposureResults, "Exposure results cannot be null");
        Objects.requireNonNull(batchErrors, "Batch errors cannot be null");
        
        if (exposureResults.isEmpty()) {
            return empty();
        }
        
        // Collect all errors
        List<ValidationError> allErrors = new ArrayList<>();
        for (ExposureValidationResult result : exposureResults.values()) {
            allErrors.addAll(result.errors());
        }
        allErrors.addAll(batchErrors);
        
        // Calculate dimension scores
        DimensionScores dimensionScores = calculateDimensionScores(
            exposureResults, 
            batchErrors, 
            exposureResults.size()
        );
        
        return builder()
            .exposureResults(exposureResults)
            .batchErrors(batchErrors)
            .allErrors(allErrors)
            .dimensionScores(dimensionScores)
            .totalExposures(exposureResults.size())
            .validExposures(countValidExposures(exposureResults))
            .build();
    }
    

    
    /**
     * Calculates dimension scores based on validation results.
     * 
     * <p>Scores are calculated as percentages: (valid exposures / total exposures) * 100</p>
     */
    private static DimensionScores calculateDimensionScores(
        Map<String, ExposureValidationResult> exposureResults,
        List<ValidationError> batchErrors,
        int totalExposures
    ) {
        if (totalExposures == 0) {
            return new DimensionScores(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Count exposures with errors per dimension
        Map<QualityDimension, Integer> errorCounts = new HashMap<>();
        for (QualityDimension dimension : QualityDimension.values()) {
            errorCounts.put(dimension, 0);
        }
        
        // Count exposure-level errors
        for (ExposureValidationResult result : exposureResults.values()) {
            for (Map.Entry<QualityDimension, List<ValidationError>> entry : result.dimensionErrors().entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    QualityDimension dimension = entry.getKey();
                    errorCounts.put(dimension, errorCounts.get(dimension) + 1);
                }
            }
        }
        
        // Count batch-level errors
        for (ValidationError error : batchErrors) {
            QualityDimension dimension = error.dimension();
            errorCounts.put(dimension, errorCounts.get(dimension) + 1);
        }
        
        // Calculate scores
        return new DimensionScores(
            calculateDimensionScore(errorCounts.get(QualityDimension.COMPLETENESS), totalExposures),
            calculateDimensionScore(errorCounts.get(QualityDimension.ACCURACY), totalExposures),
            calculateDimensionScore(errorCounts.get(QualityDimension.CONSISTENCY), totalExposures),
            calculateDimensionScore(errorCounts.get(QualityDimension.TIMELINESS), totalExposures),
            calculateDimensionScore(errorCounts.get(QualityDimension.UNIQUENESS), totalExposures),
            calculateDimensionScore(errorCounts.get(QualityDimension.VALIDITY), totalExposures)
        );
    }
    
    /**
     * Calculates score for a single dimension as percentage of valid exposures.
     */
    private static double calculateDimensionScore(int errorCount, int totalExposures) {
        if (totalExposures == 0) {
            return 0.0;
        }
        int validExposures = Math.max(0, totalExposures - errorCount);
        return (double) validExposures / totalExposures * 100.0;
    }
    
    /**
     * Counts valid exposures (no errors).
     */
    private static int countValidExposures(Map<String, ExposureValidationResult> exposureResults) {
        return (int) exposureResults.values().stream()
            .filter(ExposureValidationResult::isValid)
            .count();
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

