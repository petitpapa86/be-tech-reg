package com.bcbs239.regtech.dataquality.domain.validation;

import com.bcbs239.regtech.core.domain.specifications.Specification;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.DimensionScores;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.specifications.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Value object representing the complete validation results for a batch of exposures.
 * Contains both individual exposure results and batch-level validation information.
 * 
 * <p>This is a value object with a static factory method that knows how to create itself
 * by applying domain specifications. This is proper DDD - the value object encapsulates
 * the logic for its own creation.</p>
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
     * Factory method to validate exposures and create a ValidationResult.
     * 
     * <p>This is the proper DDD approach - the value object knows how to create itself
     * by applying domain specifications. This method orchestrates:</p>
     * <ol>
     *   <li>Validating each exposure individually using dimension specifications</li>
     *   <li>Performing batch-level validation (uniqueness)</li>
     *   <li>Calculating dimension scores</li>
     *   <li>Building the complete ValidationResult</li>
     * </ol>
     * 
     * @param exposures List of exposure records to validate
     * @return ValidationResult containing all validation errors and dimension scores
     */
    public static ValidationResult validate(List<ExposureRecord> exposures) {
        Objects.requireNonNull(exposures, "Exposures list cannot be null");
        
        if (exposures.isEmpty()) {
            return empty();
        }
        
        Map<String, ExposureValidationResult> exposureResults = new HashMap<>();
        List<ValidationError> allErrors = new ArrayList<>();
        
        // Validate each exposure individually
        for (ExposureRecord exposure : exposures) {
            ExposureValidationResult result = validateSingleExposure(exposure);
            exposureResults.put(exposure.exposureId(), result);
            allErrors.addAll(result.errors());
        }
        
        // Batch-level validation (uniqueness)
        List<ValidationError> batchErrors = validateBatchLevel(exposures);
        allErrors.addAll(batchErrors);
        
        // Calculate dimension scores
        DimensionScores dimensionScores = calculateDimensionScores(exposureResults, batchErrors, exposures.size());
        
        return builder()
            .exposureResults(exposureResults)
            .batchErrors(batchErrors)
            .allErrors(allErrors)
            .dimensionScores(dimensionScores)
            .totalExposures(exposures.size())
            .validExposures(countValidExposures(exposureResults))
            .build();
    }
    
    /**
     * Validates a single exposure across all quality dimensions using specifications.
     */
    private static ExposureValidationResult validateSingleExposure(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        Map<QualityDimension, List<ValidationError>> dimensionErrors = new HashMap<>();
        
        // Completeness validation
        List<ValidationError> completenessErrors = validateCompleteness(exposure);
        errors.addAll(completenessErrors);
        dimensionErrors.put(QualityDimension.COMPLETENESS, completenessErrors);
        
        // Accuracy validation
        List<ValidationError> accuracyErrors = validateAccuracy(exposure);
        errors.addAll(accuracyErrors);
        dimensionErrors.put(QualityDimension.ACCURACY, accuracyErrors);
        
        // Consistency validation
        List<ValidationError> consistencyErrors = validateConsistency(exposure);
        errors.addAll(consistencyErrors);
        dimensionErrors.put(QualityDimension.CONSISTENCY, consistencyErrors);
        
        // Timeliness validation
        List<ValidationError> timelinessErrors = validateTimeliness(exposure);
        errors.addAll(timelinessErrors);
        dimensionErrors.put(QualityDimension.TIMELINESS, timelinessErrors);
        
        // Validity validation
        List<ValidationError> validityErrors = validateValidity(exposure);
        errors.addAll(validityErrors);
        dimensionErrors.put(QualityDimension.VALIDITY, validityErrors);
        
        return ExposureValidationResult.builder()
            .exposureId(exposure.exposureId())
            .errors(errors)
            .dimensionErrors(dimensionErrors)
            .isValid(errors.isEmpty())
            .build();
    }
    
    /**
     * Validates batch-level constraints using specifications.
     */
    private static List<ValidationError> validateBatchLevel(List<ExposureRecord> exposures) {
        List<ValidationError> batchErrors = new ArrayList<>();
        
        // Uniqueness validation (batch-level)
        Specification<List<ExposureRecord>> uniquenessSpec = 
            UniquenessSpecifications.hasUniqueExposureIds()
                .and(UniquenessSpecifications.hasUniqueCounterpartyExposurePairs())
                .and(UniquenessSpecifications.hasUniqueReferenceNumbers());
        
        Result<Void> result = uniquenessSpec.isSatisfiedBy(exposures);
        if (!result.isSuccess()) {
            batchErrors.addAll(result.errors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.UNIQUENESS, null))
                .collect(Collectors.toList()));
        }
        
        return batchErrors;
    }
    
    /**
     * Validates completeness dimension using specifications.
     */
    private static List<ValidationError> validateCompleteness(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<ExposureRecord> completenessSpec = 
            CompletenessSpecifications.hasRequiredFields()
                .and(CompletenessSpecifications.hasLeiForCorporates())
                .and(CompletenessSpecifications.hasMaturityForTermExposures())
                .and(CompletenessSpecifications.hasInternalRating());
        
        Result<Void> result = completenessSpec.isSatisfiedBy(exposure);
        if (!result.isSuccess()) {
            errors.addAll(result.errors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.COMPLETENESS, exposure.exposureId()))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Validates accuracy dimension using specifications.
     */
    private static List<ValidationError> validateAccuracy(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<ExposureRecord> accuracySpec = 
            AccuracySpecifications.hasPositiveAmount()
                .and(AccuracySpecifications.hasValidCurrency())
                .and(AccuracySpecifications.hasValidCountry())
                .and(AccuracySpecifications.hasValidLeiFormat())
                .and(AccuracySpecifications.hasReasonableAmount());
        
        Result<Void> result = accuracySpec.isSatisfiedBy(exposure);
        if (!result.isSuccess()) {
            errors.addAll(result.errors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.ACCURACY, exposure.exposureId()))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Validates consistency dimension using specifications.
     */
    private static List<ValidationError> validateConsistency(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<ExposureRecord> consistencySpec = 
            ConsistencySpecifications.currencyMatchesCountry()
                .and(ConsistencySpecifications.sectorMatchesCounterpartyType())
                .and(ConsistencySpecifications.ratingMatchesRiskCategory())
                .and(ConsistencySpecifications.productTypeMatchesMaturity());
        
        Result<Void> result = consistencySpec.isSatisfiedBy(exposure);
        if (!result.isSuccess()) {
            errors.addAll(result.errors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.CONSISTENCY, exposure.exposureId()))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Validates timeliness dimension using specifications.
     */
    private static List<ValidationError> validateTimeliness(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<ExposureRecord> timelinessSpec = 
            TimelinessSpecifications.isWithinReportingPeriod()
                .and(TimelinessSpecifications.hasRecentValuation())
                .and(TimelinessSpecifications.isNotFutureDate())
                .and(TimelinessSpecifications.isWithinProcessingWindow());
        
        Result<Void> result = timelinessSpec.isSatisfiedBy(exposure);
        if (!result.isSuccess()) {
            errors.addAll(result.errors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.TIMELINESS, exposure.exposureId()))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Validates validity dimension using specifications.
     */
    private static List<ValidationError> validateValidity(ExposureRecord exposure) {
        // ValiditySpecifications not yet implemented
        return new ArrayList<>();
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

