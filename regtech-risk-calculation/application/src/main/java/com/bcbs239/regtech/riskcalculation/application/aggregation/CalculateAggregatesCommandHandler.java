package com.bcbs239.regtech.riskcalculation.application.aggregation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationService.AggregationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Command handler for calculating aggregates and concentration indices.
 * Uses DDD approach by delegating to domain concentration calculator.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateAggregatesCommandHandler {
    
    private final ConcentrationCalculationService concentrationCalculationService;
    
    /**
     * Handles the calculation of aggregates and concentration indices.
     * 
     * @param command The calculate aggregates command
     * @return Result containing the aggregation results or error details
     */
    public Result<AggregationResult> handle(CalculateAggregatesCommand command) {
        log.info("Starting aggregation calculation for {} exposures in batch: {}", 
            command.getExposureCount(), command.batchId().value());
        
        try {
            // Delegate to concentration calculation service
            Result<AggregationResult> result = concentrationCalculationService.calculateAggregates(
                command.classifiedExposures());
            
            if (result.isFailure()) {
                log.error("Failed to calculate aggregates for batch: {}", command.batchId().value());
                return result;
            }
            
            log.info("Successfully calculated aggregates for batch: {}", command.batchId().value());
            return result;
            
        } catch (Exception e) {
            log.error("Failed to calculate aggregates for batch: {}", command.batchId().value(), e);
            
            return Result.failure(ErrorDetail.of(
                "AGGREGATE_CALCULATION_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to calculate aggregates: " + e.getMessage(),
                "aggregate.calculation.error"
            ));
        }
    }
}