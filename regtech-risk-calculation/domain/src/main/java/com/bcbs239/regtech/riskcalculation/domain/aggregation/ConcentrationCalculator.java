package com.bcbs239.regtech.riskcalculation.domain.aggregation;

import com.bcbs239.regtech.riskcalculation.domain.calculation.GeographicBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.calculation.SectorBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.calculation.ConcentrationIndices;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalAmountEur;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Domain service for calculating concentration metrics
 * Implements business logic for computing Herfindahl-Hirschman indices
 */
@Service
public class ConcentrationCalculator {
    
    /**
     * Calculate concentration indices from geographic and sector breakdowns
     */
    public ConcentrationIndices calculateConcentrationIndices(GeographicBreakdown geographicBreakdown,
                                                            SectorBreakdown sectorBreakdown) {
        Objects.requireNonNull(geographicBreakdown, "Geographic breakdown cannot be null");
        Objects.requireNonNull(sectorBreakdown, "Sector breakdown cannot be null");
        
        HerfindahlIndex geographicHHI = calculateGeographicHerfindahl(geographicBreakdown);
        HerfindahlIndex sectorHHI = calculateSectorHerfindahl(sectorBreakdown);
        
        return new ConcentrationIndices(geographicHHI, sectorHHI);
    }
    
    /**
     * Calculate Herfindahl-Hirschman Index for geographic concentration
     */
    public HerfindahlIndex calculateGeographicHerfindahl(GeographicBreakdown breakdown) {
        Objects.requireNonNull(breakdown, "Geographic breakdown cannot be null");
        
        Map<String, TotalAmountEur> geographicAmounts = new HashMap<>();
        geographicAmounts.put("ITALY", breakdown.italyAmount());
        geographicAmounts.put("EU_OTHER", breakdown.euOtherAmount());
        geographicAmounts.put("NON_EUROPEAN", breakdown.nonEuropeanAmount());
        
        TotalAmountEur total = breakdown.getTotalAmount();
        
        return HerfindahlIndex.calculate(geographicAmounts, total);
    }
    
    /**
     * Calculate Herfindahl-Hirschman Index for sector concentration
     */
    public HerfindahlIndex calculateSectorHerfindahl(SectorBreakdown breakdown) {
        Objects.requireNonNull(breakdown, "Sector breakdown cannot be null");
        
        Map<String, TotalAmountEur> sectorAmounts = new HashMap<>();
        sectorAmounts.put("RETAIL_MORTGAGE", breakdown.retailMortgageAmount());
        sectorAmounts.put("SOVEREIGN", breakdown.sovereignAmount());
        sectorAmounts.put("CORPORATE", breakdown.corporateAmount());
        sectorAmounts.put("BANKING", breakdown.bankingAmount());
        sectorAmounts.put("OTHER", breakdown.otherAmount());
        
        TotalAmountEur total = breakdown.getTotalAmount();
        
        return HerfindahlIndex.calculate(sectorAmounts, total);
    }
    
    /**
     * Calculate summary statistics for a breakdown
     */
    public ConcentrationSummary calculateSummaryStatistics(GeographicBreakdown geographicBreakdown,
                                                         SectorBreakdown sectorBreakdown) {
        Objects.requireNonNull(geographicBreakdown, "Geographic breakdown cannot be null");
        Objects.requireNonNull(sectorBreakdown, "Sector breakdown cannot be null");
        
        // Calculate total amounts and counts
        TotalAmountEur totalAmount = geographicBreakdown.getTotalAmount();
        int totalCount = geographicBreakdown.getTotalCount().count();
        
        // Find largest concentrations
        TotalAmountEur largestGeographicAmount = geographicBreakdown.getLargestRegionalAmount();
        TotalAmountEur largestSectorAmount = sectorBreakdown.getLargestSectorAmount();
        
        // Calculate concentration indices
        ConcentrationIndices indices = calculateConcentrationIndices(geographicBreakdown, sectorBreakdown);
        
        return new ConcentrationSummary(
            totalAmount,
            totalCount,
            largestGeographicAmount,
            largestSectorAmount,
            indices
        );
    }
    
    /**
     * Summary statistics for concentration analysis
     */
    public record ConcentrationSummary(
        TotalAmountEur totalAmount,
        int totalCount,
        TotalAmountEur largestGeographicAmount,
        TotalAmountEur largestSectorAmount,
        ConcentrationIndices concentrationIndices
    ) {
        public ConcentrationSummary {
            Objects.requireNonNull(totalAmount, "Total amount cannot be null");
            Objects.requireNonNull(largestGeographicAmount, "Largest geographic amount cannot be null");
            Objects.requireNonNull(largestSectorAmount, "Largest sector amount cannot be null");
            Objects.requireNonNull(concentrationIndices, "Concentration indices cannot be null");
            
            if (totalCount < 0) {
                throw new IllegalArgumentException("Total count cannot be negative");
            }
        }
    }
}