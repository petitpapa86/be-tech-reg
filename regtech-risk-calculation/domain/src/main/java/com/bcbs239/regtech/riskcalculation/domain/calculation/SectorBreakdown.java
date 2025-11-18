package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalAmountEur;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalExposures;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.PercentageOfTotal;

/**
 * Sector breakdown of exposures by economic sector
 * Immutable value object containing sector distribution metrics
 */
public record SectorBreakdown(
    TotalAmountEur retailMortgageAmount,
    PercentageOfTotal retailMortgagePercentage,
    TotalExposures retailMortgageCount,
    
    TotalAmountEur sovereignAmount,
    PercentageOfTotal sovereignPercentage,
    TotalExposures sovereignCount,
    
    TotalAmountEur corporateAmount,
    PercentageOfTotal corporatePercentage,
    TotalExposures corporateCount,
    
    TotalAmountEur bankingAmount,
    PercentageOfTotal bankingPercentage,
    TotalExposures bankingCount,
    
    TotalAmountEur otherAmount,
    PercentageOfTotal otherPercentage,
    TotalExposures otherCount
) {
    
    public SectorBreakdown {
        // Validate all components are not null
        if (retailMortgageAmount == null || retailMortgagePercentage == null || retailMortgageCount == null ||
            sovereignAmount == null || sovereignPercentage == null || sovereignCount == null ||
            corporateAmount == null || corporatePercentage == null || corporateCount == null ||
            bankingAmount == null || bankingPercentage == null || bankingCount == null ||
            otherAmount == null || otherPercentage == null || otherCount == null) {
            throw new IllegalArgumentException("All sector breakdown components must be non-null");
        }
    }
    
    /**
     * Calculate total amount across all sectors
     */
    public TotalAmountEur getTotalAmount() {
        return retailMortgageAmount
            .add(sovereignAmount)
            .add(corporateAmount)
            .add(bankingAmount)
            .add(otherAmount);
    }
    
    /**
     * Calculate total exposure count across all sectors
     */
    public TotalExposures getTotalCount() {
        return TotalExposures.of(
            retailMortgageCount.count() + 
            sovereignCount.count() + 
            corporateCount.count() + 
            bankingCount.count() + 
            otherCount.count()
        );
    }
    
    /**
     * Get the largest sector exposure by amount
     */
    public TotalAmountEur getLargestSectorAmount() {
        TotalAmountEur largest = retailMortgageAmount;
        if (sovereignAmount.isGreaterThan(largest)) {
            largest = sovereignAmount;
        }
        if (corporateAmount.isGreaterThan(largest)) {
            largest = corporateAmount;
        }
        if (bankingAmount.isGreaterThan(largest)) {
            largest = bankingAmount;
        }
        if (otherAmount.isGreaterThan(largest)) {
            largest = otherAmount;
        }
        return largest;
    }
}