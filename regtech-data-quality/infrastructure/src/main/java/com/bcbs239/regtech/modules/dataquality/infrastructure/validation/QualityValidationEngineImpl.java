package com.bcbs239.regtech.modules.dataquality.infrastructure.validation;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Specification;
import com.bcbs239.regtech.modules.dataquality.application.validation.QualityValidationEngine;
import com.bcbs239.regtech.modules.dataquality.domain.quality.DimensionScores;
import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.modules.dataquality.domain.specifications.*;
import com.bcbs239.regtech.modules.dataquality.domain.validation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementation of QualityValidationEngine that performs six-dimensional validation
 * using the Specification pattern with streaming support for large batches.
 */
@Service
public class QualityValidationEngineImpl implements QualityValidationEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityValidationEngineImpl.class);
    
    // Thread pool for parallel validation of large batches
    private final ExecutorService validationExecutor = Executors.newFixedThreadPool(4);
    
    // Batch size for streaming validation
    private static final int STREAMING_BATCH_SIZE = 1000;
    
    @Override
    public ValidationResult validateExposures(List<ExposureRecord> exposures) {
        logger.info("Starting validation of {} exposures", exposures.size());
        long startTime = System.currentTimeMillis();
        
        try {
            // Use streaming validation for large batches
            if (exposures.size() > STREAMING_BATCH_SIZE) {
                return validateExposuresStreaming(exposures);
            } else {
                return validateExposuresStandard(exposures);
            }
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Completed validation of {} exposures in {} ms", exposures.size(), duration);
        }
    }
    
    /**
     * Standard validation for smaller batches.
     */
    private ValidationResult validateExposuresStandard(List<ExposureRecord> exposures) {
        Map<String, ExposureValidationResult> exposureResults = new HashMap<>();
        List<ValidationError> allErrors = new ArrayList<>();
        
        // Individual exposure validation
        for (ExposureRecord exposure : exposures) {
            ExposureValidationResult result = validateSingleExposure(exposure);
            exposureResults.put(exposure.getExposureId(), result);
            allErrors.addAll(result.getErrors());
        }
        
        // Batch-level validation (uniqueness)
        List<ValidationError> batchErrors = validateBatchLevel(exposures);
        allErrors.addAll(batchErrors);
        
        // Calculate dimension scores
        DimensionScores dimensionScores = calculateDimensionScores(exposureResults, batchErrors, exposures.size());
        
        return ValidationResult.builder()
            .exposureResults(exposureResults)
            .batchErrors(batchErrors)
            .allErrors(allErrors)
            .dimensionScores(dimensionScores)
            .totalExposures(exposures.size())
            .validExposures(countValidExposures(exposureResults))
            .build();
    }
    
    /**
     * Streaming validation for large batches with parallel processing.
     */
    private ValidationResult validateExposuresStreaming(List<ExposureRecord> exposures) {
        logger.info("Using streaming validation for {} exposures", exposures.size());
        
        Map<String, ExposureValidationResult> exposureResults = new HashMap<>();
        List<ValidationError> allErrors = Collections.synchronizedList(new ArrayList<>());
        
        // Process exposures in batches using parallel streams
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < exposures.size(); i += STREAMING_BATCH_SIZE) {
            int endIndex = Math.min(i + STREAMING_BATCH_SIZE, exposures.size());
            List<ExposureRecord> batch = exposures.subList(i, endIndex);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Map<String, ExposureValidationResult> batchResults = new HashMap<>();
                List<ValidationError> batchErrors = new ArrayList<>();
                
                for (ExposureRecord exposure : batch) {
                    ExposureValidationResult result = validateSingleExposure(exposure);
                    batchResults.put(exposure.getExposureId(), result);
                    batchErrors.addAll(result.getErrors());
                }
                
                synchronized (exposureResults) {
                    exposureResults.putAll(batchResults);
                }
                allErrors.addAll(batchErrors);
                
            }, validationExecutor);
            
            futures.add(future);
        }
        
        // Wait for all batches to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Batch-level validation (uniqueness)
        List<ValidationError> batchErrors = validateBatchLevel(exposures);
        allErrors.addAll(batchErrors);
        
        // Calculate dimension scores
        DimensionScores dimensionScores = calculateDimensionScores(exposureResults, batchErrors, exposures.size());
        
        return ValidationResult.builder()
            .exposureResults(exposureResults)
            .batchErrors(batchErrors)
            .allErrors(allErrors)
            .dimensionScores(dimensionScores)
            .totalExposures(exposures.size())
            .validExposures(countValidExposures(exposureResults))
            .build();
    }
    
    @Override
    public ExposureValidationResult validateSingleExposure(ExposureRecord exposure) {
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
            .exposureId(exposure.getExposureId())
            .errors(errors)
            .dimensionErrors(dimensionErrors)
            .isValid(errors.isEmpty())
            .build();
    }
    
    @Override
    public List<ValidationError> validateBatchLevel(List<ExposureRecord> exposures) {
        List<ValidationError> batchErrors = new ArrayList<>();
        
        // Uniqueness validation (batch-level)
        batchErrors.addAll(validateUniqueness(exposures));
        
        return batchErrors;
    }
    
    /**
     * Validate completeness dimension using specification composition.
     */
    private List<ValidationError> validateCompleteness(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<ExposureRecord> completenessSpec = 
            CompletenessSpecifications.hasRequiredFields()
                .and(CompletenessSpecifications.hasLeiForCorporates())
                .and(CompletenessSpecifications.hasMaturityForTermExposures())
                .and(CompletenessSpecifications.hasInternalRating());
        
        Result<Void> result = completenessSpec.isSatisfiedBy(exposure);
        if (!result.isSuccess()) {
            errors.addAll(result.getErrors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.COMPLETENESS, exposure.getExposureId()))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Validate accuracy dimension using specification composition.
     */
    private List<ValidationError> validateAccuracy(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<ExposureRecord> accuracySpec = 
            AccuracySpecifications.hasPositiveAmount()
                .and(AccuracySpecifications.hasValidCurrency())
                .and(AccuracySpecifications.hasValidCountry())
                .and(AccuracySpecifications.hasValidLeiFormat())
                .and(AccuracySpecifications.hasReasonableAmount());
        
        Result<Void> result = accuracySpec.isSatisfiedBy(exposure);
        if (!result.isSuccess()) {
            errors.addAll(result.getErrors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.ACCURACY, exposure.getExposureId()))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Validate consistency dimension using specification composition.
     */
    private List<ValidationError> validateConsistency(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<ExposureRecord> consistencySpec = 
            ConsistencySpecifications.currencyMatchesCountry()
                .and(ConsistencySpecifications.sectorMatchesCounterpartyType())
                .and(ConsistencySpecifications.ratingMatchesRiskCategory())
                .and(ConsistencySpecifications.productTypeMatchesMaturity());
        
        Result<Void> result = consistencySpec.isSatisfiedBy(exposure);
        if (!result.isSuccess()) {
            errors.addAll(result.getErrors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.CONSISTENCY, exposure.getExposureId()))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Validate timeliness dimension using specification composition.
     */
    private List<ValidationError> validateTimeliness(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<ExposureRecord> timelinessSpec = 
            TimelinessSpecifications.isWithinReportingPeriod()
                .and(TimelinessSpecifications.hasRecentValuation())
                .and(TimelinessSpecifications.isNotFutureDate())
                .and(TimelinessSpecifications.isWithinProcessingWindow());
        
        Result<Void> result = timelinessSpec.isSatisfiedBy(exposure);
        if (!result.isSuccess()) {
            errors.addAll(result.getErrors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.TIMELINESS, exposure.getExposureId()))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Validate validity dimension using specification composition.
     */
    private List<ValidationError> validateValidity(ExposureRecord exposure) {
        List<ValidationError> errors = new ArrayList<>();
        
        // Note: ValiditySpecifications is not yet implemented in the domain layer
        // This is a placeholder implementation
        // TODO: Implement ValiditySpecifications in the domain layer
        
        return errors;
    }
    
    /**
     * Validate uniqueness dimension at batch level.
     */
    private List<ValidationError> validateUniqueness(List<ExposureRecord> exposures) {
        List<ValidationError> errors = new ArrayList<>();
        
        Specification<List<ExposureRecord>> uniquenessSpec = 
            UniquenessSpecifications.hasUniqueExposureIds()
                .and(UniquenessSpecifications.hasUniqueCounterpartyExposurePairs())
                .and(UniquenessSpecifications.hasUniqueReferenceNumbers());
        
        Result<Void> result = uniquenessSpec.isSatisfiedBy(exposures);
        if (!result.isSuccess()) {
            errors.addAll(result.getErrors().stream()
                .map(error -> ValidationError.fromErrorDetail(error, QualityDimension.UNIQUENESS, null))
                .collect(Collectors.toList()));
        }
        
        return errors;
    }
    
    /**
     * Calculate dimension scores based on validation results.
     */
    private DimensionScores calculateDimensionScores(
        Map<String, ExposureValidationResult> exposureResults,
        List<ValidationError> batchErrors,
        int totalExposures
    ) {
        if (totalExposures == 0) {
            return new DimensionScores(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Count errors by dimension
        Map<QualityDimension, Integer> errorCounts = new HashMap<>();
        for (QualityDimension dimension : QualityDimension.values()) {
            errorCounts.put(dimension, 0);
        }
        
        // Count exposure-level errors
        for (ExposureValidationResult result : exposureResults.values()) {
            for (Map.Entry<QualityDimension, List<ValidationError>> entry : result.getDimensionErrors().entrySet()) {
                QualityDimension dimension = entry.getKey();
                int currentCount = errorCounts.get(dimension);
                errorCounts.put(dimension, currentCount + entry.getValue().size());
            }
        }
        
        // Count batch-level errors
        for (ValidationError error : batchErrors) {
            QualityDimension dimension = error.dimension();
            int currentCount = errorCounts.get(dimension);
            errorCounts.put(dimension, currentCount + 1);
        }
        
        // Calculate scores (percentage of exposures without errors)
        double completenessScore = calculateDimensionScore(errorCounts.get(QualityDimension.COMPLETENESS), totalExposures);
        double accuracyScore = calculateDimensionScore(errorCounts.get(QualityDimension.ACCURACY), totalExposures);
        double consistencyScore = calculateDimensionScore(errorCounts.get(QualityDimension.CONSISTENCY), totalExposures);
        double timelinessScore = calculateDimensionScore(errorCounts.get(QualityDimension.TIMELINESS), totalExposures);
        double uniquenessScore = calculateDimensionScore(errorCounts.get(QualityDimension.UNIQUENESS), totalExposures);
        double validityScore = calculateDimensionScore(errorCounts.get(QualityDimension.VALIDITY), totalExposures);
        
        return new DimensionScores(
            completenessScore,
            accuracyScore,
            consistencyScore,
            timelinessScore,
            uniquenessScore,
            validityScore
        );
    }
    
    /**
     * Calculate score for a single dimension.
     */
    private double calculateDimensionScore(int errorCount, int totalExposures) {
        if (totalExposures == 0) {
            return 0.0;
        }
        
        // Score is percentage of exposures without errors
        int validExposures = Math.max(0, totalExposures - errorCount);
        return (double) validExposures / totalExposures * 100.0;
    }
    
    /**
     * Count valid exposures (exposures without any errors).
     */
    private int countValidExposures(Map<String, ExposureValidationResult> exposureResults) {
        return (int) exposureResults.values().stream()
            .filter(ExposureValidationResult::isValid)
            .count();
    }
    
    /**
     * Cleanup method to shutdown the executor service.
     */
    public void shutdown() {
        validationExecutor.shutdown();
    }
}