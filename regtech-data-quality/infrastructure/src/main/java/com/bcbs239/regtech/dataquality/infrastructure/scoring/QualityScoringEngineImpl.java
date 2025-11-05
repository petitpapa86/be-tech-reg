package com.bcbs239.regtech.dataquality.infrastructure.scoring;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.application.scoring.QualityScoringEngine;
import com.bcbs239.regtech.dataquality.domain.quality.*;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Implementation of QualityScoringEngine that calculates weighted quality scores
 * across all six dimensions and determines overall quality grades.
 */
@Service
public class QualityScoringEngineImpl implements QualityScoringEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityScoringEngineImpl.class);
    
    // Default quality weights as specified in requirements
    private final QualityWeights defaultWeights = QualityWeights.defaultWeights();
    
    @Override
    public Result<QualityScores> calculateScores(ValidationResult validationResult) {
        return calculateScoresWithWeights(validationResult, defaultWeights);
    }
    
    @Override
    public Result<QualityScores> calculateScoresWithWeights(ValidationResult validationResult, QualityWeights weights) {
        try {
            if (validationResult == null) {
                return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of("VALIDATION_RESULT_NULL", "Validation result cannot be null"));
            }

            logger.debug("Calculating quality scores for {} exposures with {} total errors",
                validationResult.totalExposures(), validationResult.allErrors().size());

            // Calculate dimension scores
            DimensionScores dimensionScores = calculateDimensionScores(validationResult);

            // Calculate overall weighted score
            double overallScore = calculateOverallScore(dimensionScores, weights);

            // Determine quality grade
            QualityGrade grade = determineGrade(overallScore);

            QualityScores scores = new QualityScores(
                dimensionScores.completeness(),
                dimensionScores.accuracy(),
                dimensionScores.consistency(),
                dimensionScores.timeliness(),
                dimensionScores.uniqueness(),
                dimensionScores.validity(),
                overallScore,
                grade
            );

            logger.debug("Calculated quality scores: overall={}, grade={}", overallScore, grade);
            return Result.success(scores);
        } catch (Exception e) {
            logger.error("Failed to calculate quality scores", e);
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of("SCORE_CALCULATION_ERROR", "Failed to calculate scores: " + e.getMessage()));
        }
    }
    
    public DimensionScores calculateDimensionScores(ValidationResult validationResult) {
        int totalExposures = validationResult.totalExposures();

        if (totalExposures == 0) {
            logger.warn("Cannot calculate dimension scores for zero exposures");
            return new DimensionScores(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Count errors by dimension from exposure-level validation
        Map<QualityDimension, Integer> exposureErrorCounts = countExposureErrorsByDimension(
            validationResult.exposureResults());

        // Count errors by dimension from batch-level validation
        Map<QualityDimension, Integer> batchErrorCounts = countBatchErrorsByDimension(
            validationResult.batchErrors());

        // Calculate individual dimension scores
        double completenessScore = calculateDimensionScore(
            exposureErrorCounts.getOrDefault(QualityDimension.COMPLETENESS, 0) +
            batchErrorCounts.getOrDefault(QualityDimension.COMPLETENESS, 0),
            totalExposures
        );
        
        double accuracyScore = calculateDimensionScore(
            exposureErrorCounts.getOrDefault(QualityDimension.ACCURACY, 0) +
            batchErrorCounts.getOrDefault(QualityDimension.ACCURACY, 0),
            totalExposures
        );
        
        double consistencyScore = calculateDimensionScore(
            exposureErrorCounts.getOrDefault(QualityDimension.CONSISTENCY, 0) +
            batchErrorCounts.getOrDefault(QualityDimension.CONSISTENCY, 0),
            totalExposures
        );
        
        double timelinessScore = calculateDimensionScore(
            exposureErrorCounts.getOrDefault(QualityDimension.TIMELINESS, 0) +
            batchErrorCounts.getOrDefault(QualityDimension.TIMELINESS, 0),
            totalExposures
        );
        
        double uniquenessScore = calculateDimensionScore(
            exposureErrorCounts.getOrDefault(QualityDimension.UNIQUENESS, 0) +
            batchErrorCounts.getOrDefault(QualityDimension.UNIQUENESS, 0),
            totalExposures
        );
        
        double validityScore = calculateDimensionScore(
            exposureErrorCounts.getOrDefault(QualityDimension.VALIDITY, 0) +
            batchErrorCounts.getOrDefault(QualityDimension.VALIDITY, 0),
            totalExposures
        );
        
        return new DimensionScores(
            completenessScore,
            accuracyScore,
            consistencyScore,
            timelinessScore,
            uniquenessScore,
            validityScore
        );
    }
    
    public double calculateOverallScore(DimensionScores dimensionScores, QualityWeights weights) {
        double weightedScore = 
            (dimensionScores.completeness() * weights.completeness()) +
            (dimensionScores.accuracy() * weights.accuracy()) +
            (dimensionScores.consistency() * weights.consistency()) +
            (dimensionScores.timeliness() * weights.timeliness()) +
            (dimensionScores.uniqueness() * weights.uniqueness()) +
            (dimensionScores.validity() * weights.validity());
        
        // Ensure score is within valid range [0, 100]
        return Math.max(0.0, Math.min(100.0, weightedScore));
    }
    
    public QualityGrade determineGrade(double overallScore) {
        return QualityGrade.fromScore(overallScore);
    }
    
    /**
     * Calculate score for a single dimension.
     * Score represents the percentage of exposures that passed validation for this dimension.
     * 
     * @param errorCount number of errors in this dimension
     * @param totalExposures total number of exposures
     * @return dimension score as percentage (0-100)
     */
    private double calculateDimensionScore(int errorCount, int totalExposures) {
        if (totalExposures == 0) {
            return 0.0;
        }
        
        // For exposure-level errors, each error affects one exposure
        // For batch-level errors (like uniqueness), the error affects the entire batch
        int affectedExposures = Math.min(errorCount, totalExposures);
        int validExposures = totalExposures - affectedExposures;
        
        double score = (double) validExposures / totalExposures * 100.0;
        
        // Ensure score is within valid range
        return Math.max(0.0, Math.min(100.0, score));
    }
    
    /**
     * Count errors by dimension from exposure-level validation results.
     */
    private Map<QualityDimension, Integer> countExposureErrorsByDimension(
        Map<String, ExposureValidationResult> exposureResults) {
        
        Map<QualityDimension, Integer> errorCounts = new java.util.HashMap<>();
        
        // Initialize counts
        for (QualityDimension dimension : QualityDimension.values()) {
            errorCounts.put(dimension, 0);
        }
        
        // Count errors by dimension
        for (ExposureValidationResult result : exposureResults.values()) {
            for (Map.Entry<QualityDimension, List<ValidationError>> entry : result.dimensionErrors().entrySet()) {
                QualityDimension dimension = entry.getKey();
                int errorCount = entry.getValue().size();
                
                // For exposure-level validation, we count the number of exposures with errors
                // rather than the total number of errors
                if (errorCount > 0) {
                    errorCounts.put(dimension, errorCounts.get(dimension) + 1);
                }
            }
        }
        
        return errorCounts;
    }
    
    /**
     * Count errors by dimension from batch-level validation results.
     */
    private Map<QualityDimension, Integer> countBatchErrorsByDimension(List<ValidationError> batchErrors) {
        Map<QualityDimension, Integer> errorCounts = new java.util.HashMap<>();
        
        // Initialize counts
        for (QualityDimension dimension : QualityDimension.values()) {
            errorCounts.put(dimension, 0);
        }
        
        // Count batch errors by dimension
        for (ValidationError error : batchErrors) {
            QualityDimension dimension = error.dimension();
            errorCounts.put(dimension, errorCounts.get(dimension) + 1);
        }
        
        return errorCounts;
    }
    
    /**
     * Calculate compliance status based on overall score.
     * A batch is considered compliant if the overall score meets the minimum threshold.
     * 
     * @param overallScore the overall quality score
     * @return true if compliant, false otherwise
     */
    public boolean isCompliant(double overallScore) {
        // Compliance threshold: minimum 70% overall score (ACCEPTABLE grade)
        return overallScore >= 70.0;
    }
    
    /**
     * Calculate compliance status based on quality scores.
     * 
     * @param qualityScores the quality scores
     * @return true if compliant, false otherwise
     */
    public boolean isCompliant(QualityScores qualityScores) {
        return isCompliant(qualityScores.overallScore());
    }
    
    /**
     * Get quality score statistics for reporting purposes.
     * 
     * @param validationResult the validation result
     * @return quality score statistics
     */
    public QualityScoreStatistics getScoreStatistics(ValidationResult validationResult) {
        QualityScores scores = calculateScoresWithWeights(validationResult, defaultWeights).getValueOrThrow();
        DimensionScores dimensionScores = calculateDimensionScores(validationResult);
        
        return new QualityScoreStatistics(
            scores,
            dimensionScores,
            validationResult.totalExposures(),
            validationResult.validExposures(),
            validationResult.allErrors().size(),
            isCompliant(scores)
        );
    }
    
    /**
     * Inner class to hold quality score statistics.
     */
    public static class QualityScoreStatistics {
        private final QualityScores qualityScores;
        private final DimensionScores dimensionScores;
        private final int totalExposures;
        private final int validExposures;
        private final int totalErrors;
        private final boolean compliant;
        
        public QualityScoreStatistics(
            QualityScores qualityScores,
            DimensionScores dimensionScores,
            int totalExposures,
            int validExposures,
            int totalErrors,
            boolean compliant
        ) {
            this.qualityScores = qualityScores;
            this.dimensionScores = dimensionScores;
            this.totalExposures = totalExposures;
            this.validExposures = validExposures;
            this.totalErrors = totalErrors;
            this.compliant = compliant;
        }
        
        // Getters
        public QualityScores getQualityScores() { return qualityScores; }
        public DimensionScores getDimensionScores() { return dimensionScores; }
        public int getTotalExposures() { return totalExposures; }
        public int getValidExposures() { return validExposures; }
        public int getTotalErrors() { return totalErrors; }
        public boolean isCompliant() { return compliant; }
        
        public double getErrorRate() {
            return totalExposures > 0 ? (double) totalErrors / totalExposures * 100.0 : 0.0;
        }
        
        public double getValidExposureRate() {
            return totalExposures > 0 ? (double) validExposures / totalExposures * 100.0 : 0.0;
        }
    }
}