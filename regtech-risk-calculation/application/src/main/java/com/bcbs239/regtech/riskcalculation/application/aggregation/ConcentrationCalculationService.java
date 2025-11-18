package com.bcbs239.regtech.riskcalculation.application.aggregation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationService.AggregationResult;
import com.bcbs239.regtech.riskcalculation.domain.aggregation.ConcentrationCalculator;
import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import com.bcbs239.regtech.riskcalculation.domain.calculation.ConcentrationIndices;
import com.bcbs239.regtech.riskcalculation.domain.calculation.GeographicBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.calculation.SectorBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.SectorCategory;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.AmountEur;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.PercentageOfTotal;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalAmountEur;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalExposures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for calculating concentration metrics and summary statistics.
 * Uses DDD approach by delegating to domain concentration calculator.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConcentrationCalculationService {
    
    private final ConcentrationCalculator concentrationCalculator;
    
    /**
     * Calculates aggregates including geographic breakdown, sector breakdown,
     * and concentration indices using domain logic.
     * 
     * @param exposures The classified exposures to aggregate
     * @return Result containing the aggregation results
     */
    public Result<AggregationResult> calculateAggregates(List<CalculatedExposure> exposures) {
        log.debug("Calculating aggregates for {} exposures", exposures.size());
        
        try {
            // Calculate total portfolio amount
            TotalAmountEur totalAmount = calculateTotalAmount(exposures);
            
            // Calculate geographic breakdown
            GeographicBreakdown geographicBreakdown = calculateGeographicBreakdown(exposures, totalAmount);
            
            // Calculate sector breakdown
            SectorBreakdown sectorBreakdown = calculateSectorBreakdown(exposures, totalAmount);
            
            // Ask domain calculator to calculate concentration indices
            ConcentrationIndices concentrationIndices = concentrationCalculator.calculateConcentrationIndices(
                geographicBreakdown, sectorBreakdown);
            
            // Update exposure percentages
            updateExposurePercentages(exposures, totalAmount);
            
            AggregationResult result = new AggregationResult(
                totalAmount,
                geographicBreakdown,
                sectorBreakdown,
                concentrationIndices
            );
            
            log.debug("Successfully calculated aggregates with total amount: {}", totalAmount.value());
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("Failed to calculate aggregates", e);
            
            return Result.failure(ErrorDetail.of(
                "CONCENTRATION_CALCULATION_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to calculate concentration metrics: " + e.getMessage(),
                "concentration.calculation.error"
            ));
        }
    }
    
    /**
     * Calculates the total portfolio amount in EUR
     */
    private TotalAmountEur calculateTotalAmount(List<CalculatedExposure> exposures) {
        BigDecimal total = exposures.stream()
            .map(exposure -> exposure.getAmountEur().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return new TotalAmountEur(total);
    }
    
    /**
     * Calculates geographic breakdown by region
     */
    private GeographicBreakdown calculateGeographicBreakdown(List<CalculatedExposure> exposures, TotalAmountEur totalAmount) {
        Map<GeographicRegion, List<CalculatedExposure>> groupedByRegion = exposures.stream()
            .collect(Collectors.groupingBy(CalculatedExposure::getGeographicRegion));
        
        return new GeographicBreakdown(
            calculateRegionTotalAmount(groupedByRegion, GeographicRegion.ITALY),
            calculateRegionPercentage(groupedByRegion, GeographicRegion.ITALY, totalAmount),
            calculateRegionTotalCount(groupedByRegion, GeographicRegion.ITALY),
            calculateRegionTotalAmount(groupedByRegion, GeographicRegion.EU_OTHER),
            calculateRegionPercentage(groupedByRegion, GeographicRegion.EU_OTHER, totalAmount),
            calculateRegionTotalCount(groupedByRegion, GeographicRegion.EU_OTHER),
            calculateRegionTotalAmount(groupedByRegion, GeographicRegion.NON_EUROPEAN),
            calculateRegionPercentage(groupedByRegion, GeographicRegion.NON_EUROPEAN, totalAmount),
            calculateRegionTotalCount(groupedByRegion, GeographicRegion.NON_EUROPEAN)
        );
    }
    
    /**
     * Calculates sector breakdown by category
     */
    private SectorBreakdown calculateSectorBreakdown(List<CalculatedExposure> exposures, TotalAmountEur totalAmount) {
        Map<SectorCategory, List<CalculatedExposure>> groupedBySector = exposures.stream()
            .collect(Collectors.groupingBy(CalculatedExposure::getSectorCategory));
        
        return new SectorBreakdown(
            calculateSectorTotalAmount(groupedBySector, SectorCategory.RETAIL_MORTGAGE),
            calculateSectorPercentage(groupedBySector, SectorCategory.RETAIL_MORTGAGE, totalAmount),
            calculateSectorTotalCount(groupedBySector, SectorCategory.RETAIL_MORTGAGE),
            calculateSectorTotalAmount(groupedBySector, SectorCategory.SOVEREIGN),
            calculateSectorPercentage(groupedBySector, SectorCategory.SOVEREIGN, totalAmount),
            calculateSectorTotalCount(groupedBySector, SectorCategory.SOVEREIGN),
            calculateSectorTotalAmount(groupedBySector, SectorCategory.CORPORATE),
            calculateSectorPercentage(groupedBySector, SectorCategory.CORPORATE, totalAmount),
            calculateSectorTotalCount(groupedBySector, SectorCategory.CORPORATE),
            calculateSectorTotalAmount(groupedBySector, SectorCategory.BANKING),
            calculateSectorPercentage(groupedBySector, SectorCategory.BANKING, totalAmount),
            calculateSectorTotalCount(groupedBySector, SectorCategory.BANKING),
            calculateSectorTotalAmount(groupedBySector, SectorCategory.OTHER),
            calculateSectorPercentage(groupedBySector, SectorCategory.OTHER, totalAmount),
            calculateSectorTotalCount(groupedBySector, SectorCategory.OTHER)
        );
    }
    
    /**
     * Updates percentage of total for each exposure
     */
    private void updateExposurePercentages(List<CalculatedExposure> exposures, TotalAmountEur totalAmount) {
        exposures.forEach(exposure -> 
            exposure.calculatePercentage(totalAmount));
    }
    
    // Helper methods for region calculations
    private TotalAmountEur calculateRegionTotalAmount(Map<GeographicRegion, List<CalculatedExposure>> grouped, GeographicRegion region) {
        BigDecimal total = grouped.getOrDefault(region, List.of()).stream()
            .map(exposure -> exposure.getAmountEur().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TotalAmountEur(total);
    }
    
    private TotalExposures calculateRegionTotalCount(Map<GeographicRegion, List<CalculatedExposure>> grouped, GeographicRegion region) {
        return TotalExposures.of(grouped.getOrDefault(region, List.of()).size());
    }
    
    private PercentageOfTotal calculateRegionPercentage(Map<GeographicRegion, List<CalculatedExposure>> grouped, 
                                               GeographicRegion region, TotalAmountEur totalAmount) {
        TotalAmountEur regionAmount = calculateRegionTotalAmount(grouped, region);
        return PercentageOfTotal.calculate(regionAmount, totalAmount);
    }
    
    // Helper methods for sector calculations
    private TotalAmountEur calculateSectorTotalAmount(Map<SectorCategory, List<CalculatedExposure>> grouped, SectorCategory sector) {
        BigDecimal total = grouped.getOrDefault(sector, List.of()).stream()
            .map(exposure -> exposure.getAmountEur().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TotalAmountEur(total);
    }
    
    private TotalExposures calculateSectorTotalCount(Map<SectorCategory, List<CalculatedExposure>> grouped, SectorCategory sector) {
        return TotalExposures.of(grouped.getOrDefault(sector, List.of()).size());
    }
    
    private PercentageOfTotal calculateSectorPercentage(Map<SectorCategory, List<CalculatedExposure>> grouped, 
                                               SectorCategory sector, TotalAmountEur totalAmount) {
        TotalAmountEur sectorAmount = calculateSectorTotalAmount(grouped, sector);
        return PercentageOfTotal.calculate(sectorAmount, totalAmount);
    }
}