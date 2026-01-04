package com.bcbs239.regtech.riskcalculation.application.calculation;

import java.util.List;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.riskcalculation.domain.calculation.*;
import com.bcbs239.regtech.riskcalculation.domain.shared.IPerformanceMetrics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.domain.services.ExposureProcessingService;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Command handler for calculating risk metrics.
 * 
 * Acts as an orchestrator that uses domain objects directly, following the 
 * "Tell, Don't Ask" principle. Domain objects know what they can do.
 * 
 * Optimized Workflow:
 * 1. Download and parse batch data directly to domain objects (single-pass optimization)
 * 2. Create Batch aggregate 
 * 3. Process exposures through domain service (already in domain object form)
 * 4. Apply mitigations via ProtectedExposure.calculate()
 * 5. Classify exposures using ExposureClassifier
 * 6. Create classified exposures via ClassifiedExposure.of()
 * 7. Analyze portfolio using PortfolioAnalysis.analyze()
 * 8. Store results and complete batch
 * 
 * Performance Note: The parser now converts DTOs to domain objects in one pass,
 * eliminating redundant streaming/conversion in the command handler.
 * 
 * Requirements: 6.1, 7.1, 8.1
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateRiskMetricsCommandHandler {

    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final BatchRepository batchRepository;
    private final IFileStorageService fileStorageService;
    private final ICalculationResultsStorage calculationResultsStorageService;
    private final BaseUnitOfWork unitOfWork;
    private final IPerformanceMetrics performanceMetrics;
    private final BatchDataParsing batchDataParsing;
    private final ExposureProcessingService exposureProcessingService;

    @Transactional
    @Observed(name = "riskcalculation.batch.process", contextualName = "calculate-risk-metrics")
    public Result<Void> handle(CalculateRiskMetricsCommand command) {
        String batchId = command.getBatchId();
        performanceMetrics.recordBatchStart(batchId);

        log.info("Starting risk calculation for batch: {} from bank: {}", batchId, command.getBankId());

        Batch batch = null;
        
        try {
            // Check if batch already exists (idempotency check)
            Maybe<Batch> existingBatch = batchRepository.findById(BatchId.of(batchId));
            if (existingBatch.isPresent()) {
                log.info("Batch {} already exists, skipping duplicate calculation", batchId);
                return Result.success();
            }

            // Check if portfolio analysis already exists (idempotency check)
            // Do this early so we don't perform work and then return before persisting outbox events.
            Maybe<PortfolioAnalysis> existingAnalysis = portfolioAnalysisRepository.findByBatchId(batchId);
            if (existingAnalysis.isPresent()) {
                log.info("Portfolio analysis for batch {} already exists, skipping duplicate calculation", batchId);
                return Result.success();
            }
            
            // Download and parse batch data directly to domain objects (optimized single-pass parsing)
            Result<String> downloadResult = fileStorageService.retrieveFile(command.getS3Uri());
            if (downloadResult.isFailure()) {
                return handleDownloadFailure(batchId, downloadResult);
            }
            
            BatchDataParsing.ParsedBatchDomainData parsedData =
                batchDataParsing.parseBatchData(downloadResult.getValue().orElse(""));
            BankInfo bankInfo = parsedData.bankInfo();

            // Create and save Batch aggregate
            batch = Batch.create(batchId, bankInfo, parsedData.totalExposures(), command.getS3Uri());
            Result<Void> batchSaveResult = batchRepository.save(batch);
            if (batchSaveResult.isFailure()) {
                // Race/idempotency: another thread/process created the batch first.
                // In that case, exit without doing any further work (especially writing results files).
                if (batchSaveResult.getError().isPresent()
                    && "BATCH_ALREADY_EXISTS".equals(batchSaveResult.getError().get().getCode())) {
                    log.info("Batch {} already exists (detected during save), skipping duplicate calculation", batchId);
                    return Result.success();
                }

                return handleBatchSaveFailure(batchId, batchSaveResult);
            }

            // Persist the "started" domain event immediately.
            // This prevents losing events if a later step returns early (e.g., concurrent idempotency checks).
            unitOfWork.registerEntity(batch);
            unitOfWork.saveChanges();

            // Exposures are already parsed to domain objects - no conversion needed!
            // Mitigations are already grouped by exposure ID - ready for processing
            // This optimization eliminates redundant DTO-to-domain conversions
            
            // Process exposures through domain service - "Tell, Don't Ask"
            // Domain service knows how to process exposures through the pipeline
            ExposureProcessingService.ProcessingResult processingResult = 
                exposureProcessingService.processExposures(
                    parsedData.exposures(), 
                    parsedData.mitigationsByExposure()
                );
            
            List<ProtectedExposure> protectedExposures = processingResult.protectedExposures();
            List<ClassifiedExposure> classifiedExposures = processingResult.classifiedExposures();

            // Analyze portfolio using domain object
            PortfolioAnalysis analysis = PortfolioAnalysis.analyze(batchId, classifiedExposures, CorrelationContext.correlationId());

            // Store results
            RiskCalculationResult calculationResult = new RiskCalculationResult(
                batchId, bankInfo, protectedExposures, analysis, java.time.Instant.now());

            Result<String> storageResult = calculationResultsStorageService.storeCalculationResults(calculationResult);
            if (storageResult.isFailure()) {
                return handleStorageFailure(batch, analysis, storageResult);
            }

            // Complete batch
            batch.completeCalculation(storageResult.getValue().orElseThrow(), protectedExposures.size(), analysis.getTotalPortfolio());
            
            // Persist the completed batch metadata (status, processed_at, calculation_results_uri).
            // Without this, the DB row can remain stuck in PROCESSING even though completion events are emitted.
            Result<Void> finalBatchSaveResult = batchRepository.save(batch);
            if (finalBatchSaveResult.isFailure()) {
                log.warn("Failed to save completed batch {}: {}", batchId, finalBatchSaveResult.getError().orElse(null));
                // Continue anyway since calculation is complete
            }
            Result<Void> portfolioSaveResult = portfolioAnalysisRepository.save(analysis);
            if (portfolioSaveResult.isFailure()) {
                log.warn("Failed to save portfolio analysis {}: {}", batchId, portfolioSaveResult.getError().orElse(null));
                // Continue anyway since calculation is complete
            }

            // Register entities and save events to outbox
            unitOfWork.registerEntity(batch);
            unitOfWork.registerEntity(analysis);
            unitOfWork.saveChanges();
            
            performanceMetrics.recordBatchSuccess(batchId, protectedExposures.size());
            log.info("Risk calculation completed for batch: {}", batchId);

            return Result.success();

        } catch (BatchDataParsingException e) {
            performanceMetrics.recordBatchFailure(batchId, e.getMessage());
            return Result.failure(ErrorDetail.of("BATCH_PARSING_FAILED", ErrorType.BUSINESS_RULE_ERROR,
                "Failed to parse batch data: " + e.getMessage(), "calculation.parsing.failed"));
        } catch (Exception e) {
            return handleUnexpectedFailure(batch, batchId, e);
        }
    }

    // Helper methods for error handling

    private Result<Void> handleDownloadFailure(String batchId, Result<String> downloadResult) {
        ErrorDetail error = downloadResult.getError().orElse(
            ErrorDetail.of("FILE_DOWNLOAD_FAILED", ErrorType.SYSTEM_ERROR,
                "Failed to download exposure data", "calculation.file.download.failed")
        );
        log.error("File download failed for batch: {}", batchId);
        performanceMetrics.recordBatchFailure(batchId, error.getMessage());
        return Result.failure(error);
    }

    private Result<Void> handleBatchSaveFailure(String batchId, Result<Void> batchSaveResult) {
        log.error("Failed to save batch aggregate: {}", batchId);
        performanceMetrics.recordBatchFailure(batchId, "Batch save failed");
        return batchSaveResult;
    }

    private Result<Void> handleStorageFailure(Batch batch, PortfolioAnalysis analysis, Result<String> storageResult) {
        ErrorDetail error = storageResult.getError().orElse(
            ErrorDetail.of("STORAGE_FAILED", ErrorType.SYSTEM_ERROR,
                "Failed to store calculation results", "calculation.storage.failed")
        );
        log.error("Failed to store calculation results for batch: {}", batch.getId().value());
        
        batch.failCalculation("Failed to store calculation results: " + error.getMessage(), CorrelationContext.correlationId());
        
        batchRepository.save(batch);
        Result<Void> portfolioSaveResult = portfolioAnalysisRepository.save(analysis);
        if (portfolioSaveResult.isFailure()) {
            log.warn("Failed to save portfolio analysis in failure path {}: {}", batch.getId().value(), portfolioSaveResult.getError().orElse(null));
        }
        
        unitOfWork.registerEntity(batch);
        unitOfWork.registerEntity(analysis);
        unitOfWork.saveChanges();
        
        performanceMetrics.recordBatchFailure(batch.getId().value(), error.getMessage());
        return Result.failure(error);
    }

    private Result<Void> handleUnexpectedFailure(Batch batch, String batchId, Exception e) {
        if (batch != null) {
            try {
                batch.failCalculation("Calculation failed: " + e.getMessage(), CorrelationContext.correlationId());
                batchRepository.save(batch);
                unitOfWork.registerEntity(batch);
                unitOfWork.saveChanges();
            } catch (Exception ex) {
                log.error("Failed to mark batch as failed: {}", batchId, ex);
            }
        }
        
        log.error("Unexpected error during risk calculation for batch: {}", batchId, e);
        performanceMetrics.recordBatchFailure(batchId, e.getMessage());
        return Result.failure(ErrorDetail.of("CALCULATION_FAILED", ErrorType.SYSTEM_ERROR,
            "Risk calculation failed: " + e.getMessage(), "calculation.failed"));
    }
}