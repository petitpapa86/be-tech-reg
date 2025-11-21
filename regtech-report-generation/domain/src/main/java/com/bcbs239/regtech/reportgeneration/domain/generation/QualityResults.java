package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ComplianceStatus;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.QualityDimension;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.QualityGrade;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Quality Results domain object
 * 
 * Contains the data quality validation results retrieved from S3 storage,
 * including dimension scores, error distribution, and exposure-level results.
 * 
 * This data is used for generating the Data Quality section of comprehensive reports
 * and for generating contextual quality recommendations.
 */
@Getter
public class QualityResults {
    // Getters
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant timestamp;
    private final Integer totalExposures;
    private final Integer validExposures;
    private final Integer totalErrors;
    private final Map<QualityDimension, BigDecimal> dimensionScores;
    private final List<Object> batchErrors;
    private final List<ExposureResult> exposureResults;
    
    // Computed properties
    private final BigDecimal overallScore;
    private final QualityGrade overallGrade;
    private final ComplianceStatus complianceStatus;
    private final AttentionLevel attentionLevel;
    
    public QualityResults(
            @NonNull BatchId batchId,
            @NonNull BankId bankId,
            @NonNull Instant timestamp,
            @NonNull Integer totalExposures,
            @NonNull Integer validExposures,
            @NonNull Integer totalErrors,
            @NonNull Map<QualityDimension, BigDecimal> dimensionScores,
            @NonNull List<Object> batchErrors,
            @NonNull List<ExposureResult> exposureResults) {
        
        this.batchId = batchId;
        this.bankId = bankId;
        this.timestamp = timestamp;
        this.totalExposures = totalExposures;
        this.validExposures = validExposures;
        this.totalErrors = totalErrors;
        this.dimensionScores = dimensionScores;
        this.batchErrors = batchErrors;
        this.exposureResults = exposureResults;
        
        // Calculate derived values
        this.overallScore = calculateOverallScore();
        this.overallGrade = QualityGrade.fromScore(this.overallScore);
        this.complianceStatus = ComplianceStatus.fromScore(this.overallScore);
        this.attentionLevel = AttentionLevel.fromScore(this.overallScore);
    }
    
    /**
     * Calculate overall quality score as average of all dimension scores
     */
    private BigDecimal calculateOverallScore() {
        if (dimensionScores.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return dimensionScores.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(new BigDecimal(dimensionScores.size()), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get error distribution by dimension
     * Returns a map of dimension to error summary with count and percentage
     */
    public Map<QualityDimension, ErrorDimensionSummary> getErrorDistributionByDimension() {
        Map<QualityDimension, ErrorDimensionSummary> distribution = new HashMap<>();
        
        for (ExposureResult result : exposureResults) {
            for (ValidationError error : result.getErrors()) {
                QualityDimension dimension = QualityDimension.valueOf(error.dimension());
                ErrorDimensionSummary summary = distribution.computeIfAbsent(
                    dimension, k -> new ErrorDimensionSummary()
                );
                summary.incrementCount();
            }
        }
        
        // Calculate percentages
        if (totalErrors > 0) {
            distribution.values().forEach(summary -> {
                summary.setPercentage(
                    new BigDecimal(summary.getCount())
                        .divide(new BigDecimal(totalErrors), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                );
            });
        }
        
        return distribution;
    }
    
    /**
     * Get top N most common error types
     * Returns a list of error type codes with their occurrence counts
     */
    public List<Map.Entry<String, Long>> getTopErrorTypes(int limit) {
        return exposureResults.stream()
            .flatMap(result -> result.getErrors().stream())
            .collect(Collectors.groupingBy(
                ValidationError::ruleCode,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Inner class representing validation results for a single exposure
     */
    @Getter
    public static class ExposureResult {
        private final String exposureId;
        private final boolean valid;
        private final List<ValidationError> errors;
        
        public ExposureResult(String exposureId, boolean valid, List<ValidationError> errors) {
            this.exposureId = exposureId;
            this.valid = valid;
            this.errors = errors != null ? errors : List.of();
        }

    }

    /**
         * Inner class representing a validation error
         */
        public record ValidationError(String dimension, String ruleCode, String message, String fieldName,
                                      String severity) {

    }
    
    /**
     * Inner class for error dimension summary
     */
    @Getter
    @Setter
    public static class ErrorDimensionSummary {
        private int count;
        private BigDecimal percentage;
        
        public ErrorDimensionSummary() {
            this.count = 0;
            this.percentage = BigDecimal.ZERO;
        }
        
        public void incrementCount() {
            this.count++;
        }

    }
    
    /**
     * Attention level enum based on quality score
     */
    public enum AttentionLevel {
        CRITICAL,   // < 60%
        HIGH,       // 60-75%
        MEDIUM,     // 75-85%
        LOW;        // >= 85%
        
        public static AttentionLevel fromScore(BigDecimal score) {
            if (score.compareTo(new BigDecimal("60")) < 0) {
                return CRITICAL;
            } else if (score.compareTo(new BigDecimal("75")) < 0) {
                return HIGH;
            } else if (score.compareTo(new BigDecimal("85")) < 0) {
                return MEDIUM;
            } else {
                return LOW;
            }
        }
    }
}
