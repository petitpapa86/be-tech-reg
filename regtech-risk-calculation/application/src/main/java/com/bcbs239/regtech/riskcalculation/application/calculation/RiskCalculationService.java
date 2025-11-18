package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.aggregation.CalculateAggregatesCommand;
import com.bcbs239.regtech.riskcalculation.application.aggregation.CalculateAggregatesCommandHandler;
import com.bcbs239.regtech.riskcalculation.application.classification.ClassifyExposuresCommand;
import com.bcbs239.regtech.riskcalculation.application.classification.ClassifyExposuresCommandHandler;
import com.bcbs239.regtech.riskcalculation.application.shared.FileProcessingService;
import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchSummary;
import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import com.bcbs239.regtech.riskcalculation.domain.calculation.ConcentrationIndices;
import com.bcbs239.regtech.riskcalculation.domain.calculation.GeographicBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.calculation.IBatchSummaryRepository;
import com.bcbs239.regtech.riskcalculation.domain.calculation.SectorBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.FileStorageUri;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.TotalAmountEur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for orchestrating risk calculation workflows.
 * Coordinates file processing, classification, aggregation, and storage operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationService {
    
    private final FileProcessingService fileProcessingService;
    private final ClassifyExposuresCommandHandler classificationHandler;
    private final CalculateAggregatesCommandHandler aggregationHandler;
    private final IBatchSummaryRepository batchSummaryRepository;
    
    /**
     * Orchestrates the complete risk calculation workflow.
     * 
     * Transaction boundaries:
     * - Participates in outer transaction from command handler
     * - All database operations share the same transaction
     * - Failure at any step causes complete rollback
     * - Error status is persisted before transaction completes
     * 
     * @param command The calculate risk metrics command
     * @param batchSummary The batch summary to update
     * @return Result indicating success or failure
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> calculateRiskMetrics(CalculateRiskMetricsCommand command, BatchSummary batchSummary) {
        log.info("Starting risk calculation workflow [batchId:{},status:{}]", 
            command.batchId().value(), batchSummary.getStatus());
        
        long workflowStartTime = System.currentTimeMillis();
        
        try {
            // Step 1: Download and parse exposure data
            log.debug("Step 1: Downloading exposure data [batchId:{},sourceUri:{}]", 
                command.batchId().value(), command.sourceFileUri().uri());
            
            long downloadStartTime = System.currentTimeMillis();
            Result<List<CalculatedExposure>> exposuresResult = fileProcessingService.downloadAndParseExposures(
                command.sourceFileUri(), command.bankId());
            long downloadDuration = System.currentTimeMillis() - downloadStartTime;
            
            if (exposuresResult.isFailure()) {
                log.error("File download failed [batchId:{},duration:{}ms,error:{}]", 
                    command.batchId().value(), downloadDuration, 
                    exposuresResult.getError().get().getMessage());
                return Result.failure(exposuresResult.getError().get());
            }
            
            List<CalculatedExposure> exposures = exposuresResult.getValue().get();
            log.info("File download completed [batchId:{},exposureCount:{},duration:{}ms]", 
                command.batchId().value(), exposures.size(), downloadDuration);
            
            // Validate exposure count matches expected
            if (exposures.size() != command.expectedExposures().count()) {
                log.warn("Exposure count mismatch [batchId:{},expected:{},actual:{}]", 
                    command.batchId().value(), command.expectedExposures().count(), exposures.size());
            }
            
            // Transition to CALCULATING status
            Result<Void> calculatingResult = batchSummary.markAsCalculating();
            if (calculatingResult.isFailure()) {
                log.error("Failed to transition to CALCULATING status [batchId:{},error:{}]",
                    command.batchId().value(), calculatingResult.getError().get().getMessage());
                return calculatingResult;
            }
            
            // Persist status change
            Result<BatchSummary> statusSaveResult = batchSummaryRepository.save(batchSummary);
            if (statusSaveResult.isFailure()) {
                log.error("Failed to persist CALCULATING status [batchId:{},error:{}]",
                    command.batchId().value(), statusSaveResult.getError().get().getMessage());
                return Result.failure(statusSaveResult.getError().get());
            }
            
            // Step 2: Classify exposures (geographic and sector)
            log.debug("Step 2: Classifying exposures [batchId:{},exposureCount:{}]", 
                command.batchId().value(), exposures.size());
            
            long classificationStartTime = System.currentTimeMillis();
            ClassifyExposuresCommand classifyCommand = new ClassifyExposuresCommand(
                command.batchId(), exposures, command.bankId());
            
            Result<List<CalculatedExposure>> classifiedResult = classificationHandler.handle(classifyCommand);
            long classificationDuration = System.currentTimeMillis() - classificationStartTime;
            
            if (classifiedResult.isFailure()) {
                log.error("Classification failed [batchId:{},duration:{}ms,error:{}]", 
                    command.batchId().value(), classificationDuration,
                    classifiedResult.getError().get().getMessage());
                return Result.failure(classifiedResult.getError().get());
            }
            
            List<CalculatedExposure> classifiedExposures = classifiedResult.getValue().get();
            log.info("Classification completed [batchId:{},exposureCount:{},duration:{}ms]",
                command.batchId().value(), classifiedExposures.size(), classificationDuration);
            
            // Step 3: Calculate aggregates and concentration indices
            log.debug("Step 3: Calculating aggregates [batchId:{},exposureCount:{}]", 
                command.batchId().value(), classifiedExposures.size());
            
            long aggregationStartTime = System.currentTimeMillis();
            CalculateAggregatesCommand aggregateCommand = new CalculateAggregatesCommand(
                command.batchId(), classifiedExposures);
            
            Result<AggregationResult> aggregateResult = aggregationHandler.handle(aggregateCommand);
            long aggregationDuration = System.currentTimeMillis() - aggregationStartTime;
            
            if (aggregateResult.isFailure()) {
                log.error("Aggregation failed [batchId:{},duration:{}ms,error:{}]", 
                    command.batchId().value(), aggregationDuration,
                    aggregateResult.getError().get().getMessage());
                return Result.failure(aggregateResult.getError().get());
            }
            
            AggregationResult aggregation = aggregateResult.getValue().get();
            log.info("Aggregation completed [batchId:{},duration:{}ms]",
                command.batchId().value(), aggregationDuration);
            
            // Step 4: Store detailed results to file storage
            log.debug("Step 4: Storing detailed results [batchId:{}]", command.batchId().value());
            
            long storageStartTime = System.currentTimeMillis();
            Result<FileStorageUri> storageResult = fileProcessingService.storeCalculationResults(
                command.batchId(), classifiedExposures, aggregation);
            long storageDuration = System.currentTimeMillis() - storageStartTime;
            
            if (storageResult.isFailure()) {
                log.error("Result storage failed [batchId:{},duration:{}ms,error:{}]", 
                    command.batchId().value(), storageDuration,
                    storageResult.getError().get().getMessage());
                return Result.failure(storageResult.getError().get());
            }
            
            log.info("Result storage completed [batchId:{},uri:{},duration:{}ms]",
                command.batchId().value(), storageResult.getValue().get().uri(), storageDuration);
            
            // Step 5: Complete the batch summary with all results
            log.debug("Step 5: Completing batch summary [batchId:{}]", command.batchId().value());
            
            Result<Void> completeResult = batchSummary.completeCalculation(
                command.expectedExposures(),
                aggregation.totalAmountEur(),
                aggregation.geographicBreakdown(),
                aggregation.sectorBreakdown(),
                aggregation.concentrationIndices(),
                storageResult.getValue().get()
            );
            
            if (completeResult.isFailure()) {
                log.error("Failed to complete batch summary [batchId:{},error:{}]",
                    command.batchId().value(), completeResult.getError().get().getMessage());
                return completeResult;
            }
            
            // Save final batch summary with COMPLETED status
            Result<BatchSummary> finalSaveResult = batchSummaryRepository.save(batchSummary);
            if (finalSaveResult.isFailure()) {
                log.error("Failed to persist COMPLETED status [batchId:{},error:{}]", 
                    command.batchId().value(), finalSaveResult.getError().get().getMessage());
                return Result.failure(finalSaveResult.getError().get());
            }
            
            long workflowDuration = System.currentTimeMillis() - workflowStartTime;
            log.info("Risk calculation workflow completed successfully [batchId:{},totalDuration:{}ms,download:{}ms,classification:{}ms,aggregation:{}ms,storage:{}ms]", 
                command.batchId().value(), workflowDuration, downloadDuration, 
                classificationDuration, aggregationDuration, storageDuration);
            
            return Result.success(null);
            
        } catch (Exception e) {
            long workflowDuration = System.currentTimeMillis() - workflowStartTime;
            log.error("Unexpected error in risk calculation workflow [batchId:{},duration:{}ms,error:{}]", 
                command.batchId().value(), workflowDuration, e.getMessage(), e);
            
            return Result.failure(ErrorDetail.of(
                "RISK_CALCULATION_WORKFLOW_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Unexpected error in risk calculation workflow: " + e.getMessage(),
                "risk.calculation.workflow.error"
            ));
        }
    }
    
    /**
     * Result record for aggregation calculations
     */
    public record AggregationResult(
        TotalAmountEur totalAmountEur,
        GeographicBreakdown geographicBreakdown,
        SectorBreakdown sectorBreakdown,
        ConcentrationIndices concentrationIndices
    ) {}
}