package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchSummary;
import com.bcbs239.regtech.riskcalculation.domain.calculation.IBatchSummaryRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchSummaryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Command handler for calculating risk metrics.
 * Orchestrates the risk calculation workflow by coordinating with
 * classification and aggregation services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateRiskMetricsCommandHandler {
    
    private final IBatchSummaryRepository batchSummaryRepository;
    private final RiskCalculationService riskCalculationService;
    
    /**
     * Handles the risk metrics calculation command.
     * Creates a batch summary and orchestrates the calculation workflow.
     * 
     * Transaction boundaries:
     * - @Transactional ensures automatic rollback on any exception
     * - Error status is persisted before transaction commits
     * - Failed calculations are marked with error messages for troubleshooting
     * 
     * @param command The calculate risk metrics command
     * @return Result indicating success or failure of the operation
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> handle(CalculateRiskMetricsCommand command) {
        log.info("Starting risk calculation [batchId:{},bankId:{},sourceUri:{}]", 
            command.batchId().value(), command.bankId().value(), command.sourceFileUri().uri());
        
        BatchSummary batchSummary = null;
        
        try {
            // Step 1: Check idempotency - skip if batch already exists
            if (batchSummaryRepository.existsByBatchId(command.batchId())) {
                log.warn("Batch already exists, skipping duplicate processing [batchId:{}]", 
                    command.batchId().value());
                return Result.success(null);
            }
            
            // Step 2: Create new batch summary with PENDING status
            batchSummary = new BatchSummary(command.batchId(), command.bankId());
            
            // Step 3: Start calculation process (transitions to DOWNLOADING)
            Result<Void> startResult = batchSummary.startCalculation();
            if (startResult.isFailure()) {
                log.error("Failed to start calculation [batchId:{},error:{}]", 
                    command.batchId().value(), startResult.getError().get().getMessage());
                return startResult;
            }
            
            // Step 4: Save initial state with DOWNLOADING status
            Result<BatchSummary> saveResult = batchSummaryRepository.save(batchSummary);
            if (saveResult.isFailure()) {
                log.error("Failed to save initial batch summary [batchId:{},error:{}]", 
                    command.batchId().value(), saveResult.getError().get().getMessage());
                return Result.failure(saveResult.getError().get());
            }
            
            batchSummary = saveResult.getValue().get();
            
            // Step 5: Delegate to risk calculation service for processing
            // This will handle file download, classification, aggregation, and storage
            Result<Void> calculationResult = riskCalculationService.calculateRiskMetrics(
                command, batchSummary);
            
            if (calculationResult.isFailure()) {
                // Calculation failed - update status to FAILED with error message
                String errorMessage = calculationResult.getError().get().getMessage();
                log.error("Risk calculation failed [batchId:{},error:{}]", 
                    command.batchId().value(), errorMessage);
                
                // Mark batch as failed and persist error status
                Result<Void> failResult = batchSummary.failCalculation(errorMessage);
                if (failResult.isFailure()) {
                    log.error("Failed to mark batch as failed [batchId:{},error:{}]",
                        command.batchId().value(), failResult.getError().get().getMessage());
                }
                
                // Persist failed status (transaction will commit with FAILED status)
                Result<BatchSummary> failedSaveResult = batchSummaryRepository.save(batchSummary);
                if (failedSaveResult.isFailure()) {
                    log.error("Failed to persist error status [batchId:{},error:{}]",
                        command.batchId().value(), failedSaveResult.getError().get().getMessage());
                }
                
                // Return the original calculation failure
                return calculationResult;
            }
            
            log.info("Risk calculation completed successfully [batchId:{},status:{}]", 
                command.batchId().value(), batchSummary.getStatus());
            return Result.success(null);
            
        } catch (Exception e) {
            // Unexpected exception - ensure error status is persisted before rollback
            log.error("Unexpected error during risk calculation [batchId:{},error:{}]", 
                command.batchId().value(), e.getMessage(), e);
            
            // Attempt to mark batch as failed if we have a batch summary
            if (batchSummary != null) {
                try {
                    String errorMessage = "Unexpected error: " + e.getMessage();
                    batchSummary.failCalculation(errorMessage);
                    batchSummaryRepository.save(batchSummary);
                    log.info("Marked batch as failed due to unexpected error [batchId:{}]",
                        command.batchId().value());
                } catch (Exception saveException) {
                    log.error("Failed to persist error status after unexpected error [batchId:{},error:{}]",
                        command.batchId().value(), saveException.getMessage());
                }
            }
            
            // Transaction will rollback, but error status should be persisted
            return Result.failure(ErrorDetail.of(
                "RISK_CALCULATION_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error during risk calculation: " + e.getMessage(),
                "risk.calculation.unexpected.error"
            ));
        }
    }
}