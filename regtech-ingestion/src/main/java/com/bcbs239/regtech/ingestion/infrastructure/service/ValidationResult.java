package com.bcbs239.regtech.ingestion.infrastructure.service;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Result of file validation containing validation status, errors, warnings, and statistics.
 */
@Data
@Builder
public class ValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final int totalExposures;
    private final int uniqueExposureIds;
    private final Map<String, Long> currencyDistribution;
    private final Map<String, Long> countryDistribution;
    private final Map<String, Long> sectorDistribution;
    
    /**
     * Check if validation passed without errors.
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Check if there are any warnings.
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    /**
     * Get the number of validation errors.
     */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }
    
    /**
     * Get the number of validation warnings.
     */
    public int getWarningCount() {
        return warnings != null ? warnings.size() : 0;
    }
}