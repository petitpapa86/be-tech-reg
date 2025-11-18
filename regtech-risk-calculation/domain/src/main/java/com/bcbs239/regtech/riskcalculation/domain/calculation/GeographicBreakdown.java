package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalAmountEur;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalExposures;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.PercentageOfTotal;

/**
 * Geographic breakdown of exposures by region
 * Immutable value object containing geographic distribution metrics
 */
public record GeographicBreakdown(
    TotalAmountEur italyAmount,
    PercentageOfTotal italyPercentage,
    TotalExposures italyCount,
    
    TotalAmountEur euOtherAmount,
    PercentageOfTotal euOtherPercentage,
    TotalExposures euOtherCount,
    
    TotalAmountEur nonEuropeanAmount,
    PercentageOfTotal nonEuropeanPercentage,
    TotalExposures nonEuropeanCount
) {
    
    public GeographicBreakdown {
        // Validate all components are not null
        if (italyAmount == null || italyPercentage == null || italyCount == null ||
            euOtherAmount == null || euOtherPercentage == null || euOtherCount == null ||
            nonEuropeanAmount == null || nonEuropeanPercentage == null || nonEuropeanCount == null) {
            throw new IllegalArgumentException("All geographic breakdown components must be non-null");
        }
    }
    
    /**
     * Calculate total amount across all regions
     */
    public TotalAmountEur getTotalAmount() {
        return italyAmount.add(euOtherAmount).add(nonEuropeanAmount);
    }
    
    /**
     * Calculate total exposure count across all regions
     */
    public TotalExposures getTotalCount() {
        return TotalExposures.of(italyCount.count() + euOtherCount.count() + nonEuropeanCount.count());
    }
    
    /**
     * Get the largest regional exposure by amount
     */
    public TotalAmountEur getLargestRegionalAmount() {
        TotalAmountEur largest = italyAmount;
        if (euOtherAmount.isGreaterThan(largest)) {
            largest = euOtherAmount;
        }
        if (nonEuropeanAmount.isGreaterThan(largest)) {
            largest = nonEuropeanAmount;
        }
        return largest;
    }
}