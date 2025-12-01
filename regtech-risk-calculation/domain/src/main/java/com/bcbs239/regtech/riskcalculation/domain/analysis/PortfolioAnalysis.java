package com.bcbs239.regtech.riskcalculation.domain.analysis;

import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregate root for Portfolio Analysis bounded context
 * Represents the complete analysis of a portfolio including:
 * - Total portfolio amount
 * - Geographic breakdown with HHI
 * - Sector breakdown with HHI
 * 
 * This is the output of the portfolio analysis process
 */
public class PortfolioAnalysis {
    
    private final String batchId;
    private final EurAmount totalPortfolio;
    private final Breakdown geographicBreakdown;
    private final Breakdown sectorBreakdown;
    private final HHI geographicHHI;
    private final HHI sectorHHI;
    private final Instant analyzedAt;
    
    private PortfolioAnalysis(
        String batchId,
        EurAmount totalPortfolio,
        Breakdown geographicBreakdown,
        Breakdown sectorBreakdown,
        HHI geographicHHI,
        HHI sectorHHI,
        Instant analyzedAt
    ) {
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.totalPortfolio = Objects.requireNonNull(totalPortfolio, "Total portfolio cannot be null");
        this.geographicBreakdown = Objects.requireNonNull(geographicBreakdown, "Geographic breakdown cannot be null");
        this.sectorBreakdown = Objects.requireNonNull(sectorBreakdown, "Sector breakdown cannot be null");
        this.geographicHHI = Objects.requireNonNull(geographicHHI, "Geographic HHI cannot be null");
        this.sectorHHI = Objects.requireNonNull(sectorHHI, "Sector HHI cannot be null");
        this.analyzedAt = Objects.requireNonNull(analyzedAt, "Analyzed timestamp cannot be null");
    }
    
    /**
     * Factory method to analyze a portfolio and calculate all metrics
     * 
     * @param batchId the batch identifier
     * @param exposures list of classified exposures
     * @return PortfolioAnalysis with all calculated metrics
     */
    public static PortfolioAnalysis analyze(String batchId, List<ClassifiedExposure> exposures) {
        Objects.requireNonNull(batchId, "Batch ID cannot be null");
        Objects.requireNonNull(exposures, "Exposures list cannot be null");
        
        // Calculate total portfolio amount
        BigDecimal total = exposures.stream()
            .map(e -> e.netExposure().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        EurAmount totalPortfolio = EurAmount.of(total);
        
        // Calculate geographic breakdown
        Map<GeographicRegion, BigDecimal> geoAmounts = exposures.stream()
            .collect(Collectors.groupingBy(
                ClassifiedExposure::region,
                Collectors.mapping(
                    e -> e.netExposure().value(),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
        
        Breakdown geoBreakdown = Breakdown.from(geoAmounts, total);
        HHI geoHHI = HHI.calculate(geoBreakdown);
        
        // Calculate sector breakdown
        Map<EconomicSector, BigDecimal> sectorAmounts = exposures.stream()
            .collect(Collectors.groupingBy(
                ClassifiedExposure::sector,
                Collectors.mapping(
                    e -> e.netExposure().value(),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
        
        Breakdown sectorBreakdown = Breakdown.from(sectorAmounts, total);
        HHI sectorHHI = HHI.calculate(sectorBreakdown);
        
        return new PortfolioAnalysis(
            batchId,
            totalPortfolio,
            geoBreakdown,
            sectorBreakdown,
            geoHHI,
            sectorHHI,
            Instant.now()
        );
    }
    
    // Getters
    
    public String getBatchId() {
        return batchId;
    }
    
    public EurAmount getTotalPortfolio() {
        return totalPortfolio;
    }
    
    public Breakdown getGeographicBreakdown() {
        return geographicBreakdown;
    }
    
    public Breakdown getSectorBreakdown() {
        return sectorBreakdown;
    }
    
    public HHI getGeographicHHI() {
        return geographicHHI;
    }
    
    public HHI getSectorHHI() {
        return sectorHHI;
    }
    
    public Instant getAnalyzedAt() {
        return analyzedAt;
    }
}
