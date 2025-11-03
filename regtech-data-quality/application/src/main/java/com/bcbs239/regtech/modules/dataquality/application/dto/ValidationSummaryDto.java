package com.bcbs239.regtech.modules.dataquality.application.dto;

import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ValidationSummary;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO for validation summary with error statistics.
 * Provides a serializable representation of validation results for API responses.
 */
public record ValidationSummaryDto(
    int totalExposures,
    int validExposures,
    int invalidExposures,
    int totalErrors,
    double validationRate,
    Map<String, Integer> errorsByDimension,
    Map<String, Integer> errorsBySeverity,
    Map<String, Integer> errorsByCode,
    Map<String, Integer> topErrorCodes
) {
    
    /**
     * Creates a DTO from domain ValidationSummary.
     */
    public static ValidationSummaryDto fromDomain(ValidationSummary summary) {
        if (summary == null) {
            return empty();
        }
        
        // Convert dimension enum keys to strings
        Map<String, Integer> dimensionErrors = summary.errorsByDimension().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().name(),
                Map.Entry::getValue
            ));
        
        // Convert severity enum keys to strings
        Map<String, Integer> severityErrors = summary.errorsBySeverity().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().name(),
                Map.Entry::getValue
            ));
        
        // Get top 10 error codes
        Map<String, Integer> topErrors = summary.errorsByCode().entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
        
        double validationRate = summary.totalExposures() > 0 
            ? (double) summary.validExposures() / summary.totalExposures() * 100.0
            : 0.0;
        
        return new ValidationSummaryDto(
            summary.totalExposures(),
            summary.validExposures(),
            summary.totalExposures() - summary.validExposures(),
            summary.totalErrors(),
            validationRate,
            dimensionErrors,
            severityErrors,
            summary.errorsByCode(),
            topErrors
        );
    }
    
    /**
     * Creates an empty validation summary DTO.
     */
    public static ValidationSummaryDto empty() {
        return new ValidationSummaryDto(
            0, 0, 0, 0, 0.0,
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
    
    /**
     * Gets the validation rate as a percentage string.
     */
    public String getValidationRatePercentage() {
        return String.format("%.1f%%", validationRate);
    }
    
    /**
     * Gets the error rate as a percentage.
     */
    public double getErrorRate() {
        return 100.0 - validationRate;
    }
    
    /**
     * Gets the error rate as a percentage string.
     */
    public String getErrorRatePercentage() {
        return String.format("%.1f%%", getErrorRate());
    }
    
    /**
     * Gets the dimension with the most errors.
     */
    public String getMostProblematicDimension() {
        return errorsByDimension.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
    }
    
    /**
     * Gets the severity level with the most errors.
     */
    public String getMostCommonSeverity() {
        return errorsBySeverity.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
    }
    
    /**
     * Gets the most frequent error code.
     */
    public String getMostFrequentErrorCode() {
        return errorsByCode.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("none");
    }
    
    /**
     * Checks if the validation meets a minimum quality threshold.
     */
    public boolean meetsQualityThreshold(double minimumValidationRate) {
        return validationRate >= minimumValidationRate;
    }
    
    /**
     * Gets error count for a specific dimension.
     */
    public int getErrorCountForDimension(String dimension) {
        return errorsByDimension.getOrDefault(dimension, 0);
    }
    
    /**
     * Gets error count for a specific severity.
     */
    public int getErrorCountForSeverity(String severity) {
        return errorsBySeverity.getOrDefault(severity, 0);
    }
    
    /**
     * Gets a summary of validation quality.
     */
    public String getQualitySummary() {
        if (validationRate >= 95.0) {
            return "EXCELLENT";
        } else if (validationRate >= 90.0) {
            return "VERY_GOOD";
        } else if (validationRate >= 80.0) {
            return "GOOD";
        } else if (validationRate >= 70.0) {
            return "ACCEPTABLE";
        } else {
            return "POOR";
        }
    }
    
    /**
     * Checks if there are critical errors.
     */
    public boolean hasCriticalErrors() {
        return getErrorCountForSeverity("CRITICAL") > 0;
    }
    
    /**
     * Checks if there are warning-level errors.
     */
    public boolean hasWarnings() {
        return getErrorCountForSeverity("WARNING") > 0;
    }
}