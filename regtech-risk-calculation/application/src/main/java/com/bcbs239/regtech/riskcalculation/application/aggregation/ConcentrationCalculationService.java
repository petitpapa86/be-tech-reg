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
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalAmountEur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        
        return GeographicBreakdown.builder()
            .italyAmount(calculateRegionAmount(groupedByRegion, GeographicRegion.ITALY))
            .italyCount(calculateRegionCount(groupedByRegion, GeographicRegion.ITALY))
            .italyPercentage(calculateRegionPercentage(groupedByRegion, GeographicRegion.ITALY, totalAmount))
            .euOtherAmount(calculateRegionAmount(groupedByRegion, GeographicRegion.EU_OTHER))
            .euOtherCount(calculateRegionCount(groupedByRegion, GeographicRegion.EU_OTHER))
            .euOtherPercentage(calculateRegionPercentage(groupedByRegion, GeographicRegion.EU_OTHER, totalAmount))
            .nonEuropeanAmount(calculateRegionAmount(groupedByRegion, GeographicRegion.NON_EUROPEAN))
            .nonEuropeanCount(calculateRegionCount(groupedByRegion, GeographicRegion.NON_EUROPEAN))
            .nonEuropeanPercentage(calculateRegionPercentage(groupedByRegion, GeographicRegion.NON_EUROPEAN, totalAmount))
            .build();
    }
    
    /**
     * Calculates sector breakdown by category
     */
    private SectorBreakdown calculateSectorBreakdown(List<CalculatedExposure> exposures, TotalAmountEur totalAmount) {
        Map<SectorCategory, List<CalculatedExposure>> groupedBySector = exposures.stream()
            .collect(Collectors.groupingBy(CalculatedExposure::getSectorCategory));
        
        return SectorBreakdown.builder()
            .retailMortgageAmount(calculateSectorAmount(groupedBySector, SectorCategory.RETAIL_MORTGAGE))
            .retailMortgageCount(calculateSectorCount(groupedBySector, SectorCategory.RETAIL_MORTGAGE))
            .retailMortgagePercentage(calculateSectorPercentage(groupedBySector, SectorCategory.RETAIL_MORTGAGE, totalAmount))
            .sovereignAmount(calculateSectorAmount(groupedBySector, SectorCategory.SOVEREIGN))
            .sovereignCount(calculateSectorCount(groupedBySector, SectorCategory.SOVEREIGN))
            .sovereignPercentage(calculateSectorPercentage(groupedBySector, SectorCategory.SOVEREIGN, totalAmount))
            .corporateAmount(calculateSectorAmount(groupedBySector, SectorCategory.CORPORATE))
            .corporateCount(calculateSectorCount(groupedBySector, SectorCategory.CORPORATE))
            .corporatePercentage(calculateSectorPercentage(groupedBySector, SectorCategory.CORPORATE, totalAmount))
            .bankingAmount(calculateSectorAmount(groupedBySector, SectorCategory.BANKING))
            .bankingCount(calculateSectorCount(groupedBySector, SectorCategory.BANKING))
            .bankingPercentage(calculateSectorPercentage(groupedBySector, SectorCategory.BANKING, totalAmount))
            .otherAmount(calculateSectorAmount(groupedBySector, SectorCategory.OTHER))
            .otherCount(calculateSectorCount(groupedBySector, SectorCategory.OTHER))
            .otherPercentage(calculateSectorPercentage(groupedBySector, SectorCategory.OTHER, totalAmount))
            .build();
    }
    
    /**
     * Updates percentage of total for each exposure
     */
    private void updateExposurePercentages(List<CalculatedExposure> exposures, TotalAmountEur totalAmount) {
        exposures.forEach(exposure -> 
            exposure.calculatePercentage(totalAmount));
    }
    
    // Helper methods for region calculations
    private AmountEur calculateRegionAmount(Map<GeographicRegion, List<CalculatedExposure>> grouped, GeographicRegion region) {
        return new AmountEur(grouped.getOrDefault(region, List.of()).stream()
            .map(exposure -> exposure.getAmountEur().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    
    private int calculateRegionCount(Map<GeographicRegion, List<CalculatedExposure>> grouped, GeographicRegion region) {
        return grouped.getOrDefault(region, List.of()).size();
    }
    
    private BigDecimal calculateRegionPercentage(Map<GeographicRegion, List<CalculatedExposure>> grouped, 
                                               GeographicRegion region, TotalAmountEur totalAmount) {
        AmountEur regionAmount = calculateRegionAmount(grouped, region);
        if (totalAmount.value().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return regionAmount.value()
            .divide(totalAmount.value(), 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
    
    // Helper methods for sector calculations
    private AmountEur calculateSectorAmount(Map<SectorCategory, List<CalculatedExposure>> grouped, SectorCategory sector) {
        return new AmountEur(grouped.getOrDefault(sector, List.of()).stream()
            .map(exposure -> exposure.getAmountEur().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    
    private int calculateSectorCount(Map<SectorCategory, List<CalculatedExposure>> grouped, SectorCategory sector) {
        return grouped.getOrDefault(sector, List.of()).size();
    }
    
    private BigDecimal calculateSectorPercentage(Map<SectorCategory, List<CalculatedExposure>> grouped, 
                                               SectorCategory sector, TotalAmountEur totalAmount) {
        AmountEur sectorAmount = calculateSectorAmount(grouped, sector);
        if (totalAmount.value().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return sectorAmount.value()
            .divide(totalAmount.value(), 4, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}