package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Calculation Results domain object
 * 
 * Contains the calculation results retrieved from S3 storage, including
 * all large exposures data, capital information, and aggregated metrics.
 * 
 * This data is used as input for both HTML and XBRL report generation.
 */
public record CalculationResults(
    @NonNull BatchId batchId,
    @NonNull BankId bankId,
    @NonNull BankName bankName,
    @NonNull ReportingDate reportingDate,
    @NonNull AmountEur tierOneCapital,
    int totalExposures,
    @NonNull AmountEur totalAmount,
    int limitBreaches,
    @NonNull List<CalculatedExposure> exposures,
    @NonNull GeographicBreakdown geographicBreakdown,
    @NonNull SectorBreakdown sectorBreakdown,
    @NonNull ConcentrationIndices concentrationIndices,
    @NonNull ProcessingTimestamps processingTimestamps
) {
    
    /**
     * Compact constructor with validation
     */
    public CalculationResults {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (bankId == null) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
        if (bankName == null) {
            throw new IllegalArgumentException("Bank name cannot be null");
        }
        if (reportingDate == null) {
            throw new IllegalArgumentException("Reporting date cannot be null");
        }
        if (tierOneCapital == null) {
            throw new IllegalArgumentException("Tier 1 capital cannot be null");
        }
        if (exposures == null) {
            throw new IllegalArgumentException("Exposures list cannot be null");
        }
        if (totalExposures < 0) {
            throw new IllegalArgumentException("Total exposures count cannot be negative");
        }
        if (totalAmount == null) {
            throw new IllegalArgumentException("Total amount cannot be null");
        }
        if (limitBreaches < 0) {
            throw new IllegalArgumentException("Limit breaches count cannot be negative");
        }
        if (geographicBreakdown == null) {
            throw new IllegalArgumentException("Geographic breakdown cannot be null");
        }
        if (sectorBreakdown == null) {
            throw new IllegalArgumentException("Sector breakdown cannot be null");
        }
        if (concentrationIndices == null) {
            throw new IllegalArgumentException("Concentration indices cannot be null");
        }
        if (processingTimestamps == null) {
            throw new IllegalArgumentException("Processing timestamps cannot be null");
        }
    }
    
    /**
     * Get large exposures (≥10% of capital)
     * As per CRR Article 392, large exposures are those equal to or exceeding 10% of eligible capital
     */
    public List<CalculatedExposure> getLargeExposures() {
        return exposures.stream()
            .filter(exposure -> exposure.percentageOfCapital().isGreaterThanOrEqualTo(new BigDecimal("10")))
            .collect(Collectors.toList());
    }
    
    /**
     * Get non-compliant exposures (>25% of capital)
     * As per CRR Article 395, exposures exceeding 25% of eligible capital are non-compliant
     */
    public List<CalculatedExposure> getNonCompliantExposures() {
        return exposures.stream()
            .filter(exposure -> exposure.percentageOfCapital().isGreaterThan(new BigDecimal("25")))
            .collect(Collectors.toList());
    }

    /**
     * Get top exposures by EUR amount (descending).
     */
    public List<CalculatedExposure> getTopExposures(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return exposures.stream()
            .sorted(Comparator.comparing(CalculatedExposure::amountEur).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Convenience getter for templates.
     */
    public List<CalculatedExposure> getTop10Exposures() {
        return getTopExposures(10);
    }

    /**
     * Convenience getter for templates.
     */
    public int getLargeExposuresCount() {
        return getLargeExposures().size();
    }
    
    /**
     * Check if there are any large exposures
     */
    public boolean hasLargeExposures() {
        return !getLargeExposures().isEmpty();
    }
    
    /**
     * Check if there are any limit breaches
     */
    public boolean hasLimitBreaches() {
        return limitBreaches > 0;
    }
    
    /**
     * Get the number of compliant exposures (≤25% of capital)
     */
    public long getCompliantExposuresCount() {
        return exposures.stream()
            .filter(CalculatedExposure::compliant)
            .count();
    }
    
    /**
     * Get the number of non-compliant exposures (>25% of capital)
     */
    public long getNonCompliantExposuresCount() {
        return exposures.stream()
            .filter(exposure -> !exposure.compliant())
            .count();
    }
}
