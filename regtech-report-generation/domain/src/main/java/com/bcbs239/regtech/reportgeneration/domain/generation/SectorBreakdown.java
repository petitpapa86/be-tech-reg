package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.AmountEur;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Sector breakdown of exposures by economic sector
 * Immutable value object containing sector distribution metrics
 */
public record SectorBreakdown(
    @NonNull AmountEur retailMortgageAmount,
    @NonNull BigDecimal retailMortgagePercentage,
    int retailMortgageCount,
    
    @NonNull AmountEur sovereignAmount,
    @NonNull BigDecimal sovereignPercentage,
    int sovereignCount,
    
    @NonNull AmountEur corporateAmount,
    @NonNull BigDecimal corporatePercentage,
    int corporateCount,
    
    @NonNull AmountEur bankingAmount,
    @NonNull BigDecimal bankingPercentage,
    int bankingCount,
    
    @NonNull AmountEur otherAmount,
    @NonNull BigDecimal otherPercentage,
    int otherCount
) {
    
    /**
     * Compact constructor with validation
     */
    public SectorBreakdown {
        if (retailMortgageCount < 0 || sovereignCount < 0 || corporateCount < 0 ||
            bankingCount < 0 || otherCount < 0) {
            throw new IllegalArgumentException("Sector counts cannot be negative");
        }
    }
    
    /**
     * Create from a map of sector codes to amounts
     */
    public static SectorBreakdown fromMap(Map<String, BigDecimal> sectorMap, BigDecimal totalAmount) {
        BigDecimal retailAmt = sectorMap.getOrDefault("RETAIL_MORTGAGE", BigDecimal.ZERO);
        BigDecimal sovereignAmt = sectorMap.getOrDefault("SOVEREIGN", BigDecimal.ZERO);
        BigDecimal corporateAmt = sectorMap.getOrDefault("CORPORATE", BigDecimal.ZERO);
        BigDecimal bankingAmt = sectorMap.getOrDefault("BANKING", BigDecimal.ZERO);
        BigDecimal otherAmt = sectorMap.entrySet().stream()
            .filter(e -> !e.getKey().matches("RETAIL_MORTGAGE|SOVEREIGN|CORPORATE|BANKING"))
            .map(Map.Entry::getValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal retailPct = calculatePercentage(retailAmt, totalAmount);
        BigDecimal sovereignPct = calculatePercentage(sovereignAmt, totalAmount);
        BigDecimal corporatePct = calculatePercentage(corporateAmt, totalAmount);
        BigDecimal bankingPct = calculatePercentage(bankingAmt, totalAmount);
        BigDecimal otherPct = calculatePercentage(otherAmt, totalAmount);
        
        return new SectorBreakdown(
            AmountEur.of(retailAmt),
            retailPct,
            0, // Count would need to be calculated separately
            AmountEur.of(sovereignAmt),
            sovereignPct,
            0,
            AmountEur.of(corporateAmt),
            corporatePct,
            0,
            AmountEur.of(bankingAmt),
            bankingPct,
            0,
            AmountEur.of(otherAmt),
            otherPct,
            0
        );
    }
    
    /**
     * Calculate total amount across all sectors
     */
    public AmountEur getTotalAmount() {
        return AmountEur.of(
            retailMortgageAmount.value()
                .add(sovereignAmount.value())
                .add(corporateAmount.value())
                .add(bankingAmount.value())
                .add(otherAmount.value())
        );
    }
    
    /**
     * Calculate total exposure count across all sectors
     */
    public int getTotalCount() {
        return retailMortgageCount + sovereignCount + corporateCount + bankingCount + otherCount;
    }
    
    /**
     * Get the largest sector exposure by amount
     */
    public AmountEur getLargestSectorAmount() {
        AmountEur largest = retailMortgageAmount;
        if (sovereignAmount.value().compareTo(largest.value()) > 0) {
            largest = sovereignAmount;
        }
        if (corporateAmount.value().compareTo(largest.value()) > 0) {
            largest = corporateAmount;
        }
        if (bankingAmount.value().compareTo(largest.value()) > 0) {
            largest = bankingAmount;
        }
        if (otherAmount.value().compareTo(largest.value()) > 0) {
            largest = otherAmount;
        }
        return largest;
    }
    
    private static BigDecimal calculatePercentage(BigDecimal amount, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(total, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }
}
