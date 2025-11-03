package com.bcbs239.regtech.dataquality.domain.validation;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;

import java.util.Map;

/**
 * Value object representing aggregate statistics for validation results.
 * Provides summary information across all exposures in a batch.
 */
public record ValidationSummary(
    int totalExposures,
    int validExposures,
    int invalidExposures,
    int totalErrors,
    Map<QualityDimension, Integer> errorsByDimension,
    Map<ValidationError.ErrorSeverity, Integer> errorsBySeverity,
    Map<String, Integer> errorsByCode,
    double overallValidationRate
) {
    
    /**
     * Creates an empty validation summary
     */
    public static ValidationSummary empty() {
        return new ValidationSummary(
            0, 0, 0, 0,
            Map.of(),
            Map.of(),
            Map.of(),
            0.0
        );
    }
    
    /**
     * Creates a builder for constructing ValidationSummary instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the validation rate as a percentage (0-100)
     */
    public double getValidationRatePercentage() {
        return overallValidationRate * 100.0;
    }
    
    /**
     * Gets the error count for a specific dimension
     */
    public int getErrorCountForDimension(QualityDimension dimension) {
        return errorsByDimension.getOrDefault(dimension, 0);
    }
    
    /**
     * Gets the error count for a specific severity
     */
    public int getErrorCountForSeverity(ValidationError.ErrorSeverity severity) {
        return errorsBySeverity.getOrDefault(severity, 0);
    }
    
    /**
     * Gets the error count for a specific error code
     */
    public int getErrorCountForCode(String errorCode) {
        return errorsByCode.getOrDefault(errorCode, 0);
    }
    
    /**
     * Checks if there are any critical errors
     */
    public boolean hasCriticalErrors() {
        return getErrorCountForSeverity(ValidationError.ErrorSeverity.CRITICAL) > 0;
    }
    
    /**
     * Gets the most common error code
     */
    public String getMostCommonErrorCode() {
        return errorsByCode.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Gets the dimension with the most errors
     */
    public QualityDimension getDimensionWithMostErrors() {
        return errorsByDimension.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    /**
     * Checks if the batch meets minimum quality thresholds
     */
    public boolean meetsQualityThreshold(double minimumValidationRate) {
        return overallValidationRate >= minimumValidationRate;
    }
    
    /**
     * Builder class for ValidationSummary
     */
    public static class Builder {
        private int totalExposures;
        private int validExposures;
        private int invalidExposures;
        private int totalErrors;
        private Map<QualityDimension, Integer> errorsByDimension = Map.of();
        private Map<ValidationError.ErrorSeverity, Integer> errorsBySeverity = Map.of();
        private Map<String, Integer> errorsByCode = Map.of();
        private double overallValidationRate;
        
        public Builder totalExposures(int totalExposures) {
            this.totalExposures = totalExposures;
            return this;
        }
        
        public Builder validExposures(int validExposures) {
            this.validExposures = validExposures;
            this.invalidExposures = totalExposures - validExposures;
            this.overallValidationRate = totalExposures > 0 ? 
                (double) validExposures / totalExposures : 0.0;
            return this;
        }
        
        public Builder invalidExposures(int invalidExposures) {
            this.invalidExposures = invalidExposures;
            this.validExposures = totalExposures - invalidExposures;
            this.overallValidationRate = totalExposures > 0 ? 
                (double) validExposures / totalExposures : 0.0;
            return this;
        }
        
        public Builder totalErrors(int totalErrors) {
            this.totalErrors = totalErrors;
            return this;
        }
        
        public Builder errorsByDimension(Map<QualityDimension, Integer> errorsByDimension) {
            this.errorsByDimension = errorsByDimension != null ? errorsByDimension : Map.of();
            return this;
        }
        
        public Builder errorsBySeverity(Map<ValidationError.ErrorSeverity, Integer> errorsBySeverity) {
            this.errorsBySeverity = errorsBySeverity != null ? errorsBySeverity : Map.of();
            return this;
        }
        
        public Builder errorsByCode(Map<String, Integer> errorsByCode) {
            this.errorsByCode = errorsByCode != null ? errorsByCode : Map.of();
            return this;
        }
        
        public Builder overallValidationRate(double overallValidationRate) {
            this.overallValidationRate = overallValidationRate;
            return this;
        }
        
        public ValidationSummary build() {
            // Auto-calculate validation rate if not set
            if (overallValidationRate == 0.0 && totalExposures > 0) {
                overallValidationRate = (double) validExposures / totalExposures;
            }
            
            // Auto-calculate invalid exposures if not set
            if (invalidExposures == 0 && totalExposures > 0 && validExposures > 0) {
                invalidExposures = totalExposures - validExposures;
            }
            
            return new ValidationSummary(
                totalExposures,
                validExposures,
                invalidExposures,
                totalErrors,
                errorsByDimension,
                errorsBySeverity,
                errorsByCode,
                overallValidationRate
            );
        }
    }
}