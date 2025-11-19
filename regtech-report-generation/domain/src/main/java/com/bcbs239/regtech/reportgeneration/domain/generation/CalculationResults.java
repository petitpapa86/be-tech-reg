package com.bcbs239.regtech.reportgeneration.domain.generation;

import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Calculation Results value object
 * 
 * Contains the calculation results retrieved from S3 storage, including
 * all large exposures data, capital information, and aggregated metrics.
 * 
 * This data is used as input for both HTML and XBRL report generation.
 */
public record CalculationResults(
    @NonNull String batchId,
    @NonNull String bankId,
    @NonNull LocalDate reportingDate,
    @NonNull BigDecimal tier1Capital,
    @NonNull List<CalculatedExposure> largeExposures,
    int totalExposuresCount,
    @NonNull BigDecimal totalExposureAmount,
    int limitBreachesCount,
    @NonNull Map<String, BigDecimal> sectorBreakdown,
    @NonNull Map<String, BigDecimal> geographicBreakdown
) {
    
    /**
     * Compact constructor with validation
     */
    public CalculationResults {
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException("Batch ID cannot be null or blank");
        }
        if (bankId == null || bankId.isBlank()) {
            throw new IllegalArgumentException("Bank ID cannot be null or blank");
        }
        if (reportingDate == null) {
            throw new IllegalArgumentException("Reporting date cannot be null");
        }
        if (tier1Capital == null || tier1Capital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tier 1 capital must be positive");
        }
        if (largeExposures == null) {
            throw new IllegalArgumentException("Large exposures list cannot be null");
        }
        if (totalExposuresCount < 0) {
            throw new IllegalArgumentException("Total exposures count cannot be negative");
        }
        if (totalExposureAmount == null || totalExposureAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total exposure amount cannot be negative");
        }
        if (limitBreachesCount < 0) {
            throw new IllegalArgumentException("Limit breaches count cannot be negative");
        }
        if (sectorBreakdown == null) {
            throw new IllegalArgumentException("Sector breakdown cannot be null");
        }
        if (geographicBreakdown == null) {
            throw new IllegalArgumentException("Geographic breakdown cannot be null");
        }
    }
    
    /**
     * Check if there are any large exposures
     */
    public boolean hasLargeExposures() {
        return !largeExposures.isEmpty();
    }
    
    /**
     * Check if there are any limit breaches
     */
    public boolean hasLimitBreaches() {
        return limitBreachesCount > 0;
    }
    
    /**
     * Get the number of compliant exposures (≤25% of capital)
     */
    public long getCompliantExposuresCount() {
        return largeExposures.stream()
            .filter(CalculatedExposure::compliant)
            .count();
    }
    
    /**
     * Get the number of non-compliant exposures (>25% of capital)
     */
    public long getNonCompliantExposuresCount() {
        return largeExposures.stream()
            .filter(exposure -> !exposure.compliant())
            .count();
    }
}
