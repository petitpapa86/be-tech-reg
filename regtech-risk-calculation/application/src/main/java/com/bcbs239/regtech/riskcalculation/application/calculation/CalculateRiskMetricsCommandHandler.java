package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.riskcalculation.application.monitoring.PerformanceMetrics;
import com.bcbs239.regtech.riskcalculation.application.storage.ICalculationResultsStorageService;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.calculation.Batch;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.persistence.BatchRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.domain.services.BatchDataParsingService;
import com.bcbs239.regtech.riskcalculation.domain.services.ExposureProcessingService;
import com.bcbs239.regtech.riskcalculation.domain.services.IFileStorageService;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.observation.annotation.Observed;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command handler for calculating risk metrics.
 * 
 * Acts as an orchestrator that uses domain objects directly, following the 
 * "Tell, Don't Ask" principle. Domain objects know what they can do.
 * 
 * Workflow:
 * 1. Download and parse batch data
 * 2. Create Batch aggregate 
 * 3. Use ExposureRecording.fromDTO() to convert exposures
 * 4. Use ProtectedExposure.calculate() to apply mitigations
 * 5. Use ExposureClassifier to classify exposures
 * 6. Use ClassifiedExposure.of() to create classified exposures
 * 7. Use PortfolioAnalysis.analyze() to analyze portfolio
 * 8. Store results and complete batch
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
    private final ICalculationResultsStorageService calculationResultsStorageService;
    private final BaseUnitOfWork unitOfWork;
    private final PerformanceMetrics performanceMetrics;
    private final BatchDataParsingService batchDataParsingService;
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
            
            // Download and parse batch data
            Result<String> downloadResult = fileStorageService.retrieveFile(command.getS3Uri());
            if (downloadResult.isFailure()) {
                return handleDownloadFailure(batchId, downloadResult);
            }
            
            BatchDataParsingService.ParsedBatchData parsedData = 
                batchDataParsingService.parseBatchData(downloadResult.getValue().orElse(""));
            BankInfo bankInfo = parsedData.bankInfo();

            // Create and save Batch aggregate
            batch = Batch.create(batchId, bankInfo, parsedData.batchData().exposures().size(), command.getS3Uri());
            Result<Void> batchSaveResult = batchRepository.save(batch);
            if (batchSaveResult.isFailure()) {
                return handleBatchSaveFailure(batchId, batchSaveResult);
            }

            // Convert exposures using domain objects directly
            List<ExposureRecording> exposures = parsedData.batchData().exposures().stream()
                .map(ExposureRecording::fromDTO)
                .collect(Collectors.toList());

            // Group mitigations by exposure ID
            Map<String, List<CreditRiskMitigationDTO>> mitigationsByExposure = 
                parsedData.batchData().creditRiskMitigation().stream()
                    .collect(Collectors.groupingBy(CreditRiskMitigationDTO::exposureId));

            // Process exposures through domain service - "Tell, Don't Ask"
            // Domain service knows how to process exposures through the pipeline
            ExposureProcessingService.ProcessingResult processingResult = 
                exposureProcessingService.processExposures(exposures, mitigationsByExposure);
            
            List<ProtectedExposure> protectedExposures = processingResult.protectedExposures();
            List<ClassifiedExposure> classifiedExposures = processingResult.classifiedExposures();

            // Analyze portfolio using domain object
            PortfolioAnalysis analysis = PortfolioAnalysis.analyze(batchId, classifiedExposures);

            // Store results
            RiskCalculationResult calculationResult = new RiskCalculationResult(
                batchId, bankInfo, protectedExposures, analysis, java.time.Instant.now());

            Result<String> storageResult = calculationResultsStorageService.storeCalculationResults(calculationResult);
            if (storageResult.isFailure()) {
                return handleStorageFailure(batch, analysis, storageResult);
            }

            // Complete batch
            batch.completeCalculation(storageResult.getValue().orElseThrow(), protectedExposures.size(), analysis.getTotalPortfolio());
            batchRepository.save(batch);
            portfolioAnalysisRepository.save(analysis);

            // Save events
            unitOfWork.registerEntity(batch);
            unitOfWork.registerEntity(analysis);
            unitOfWork.saveChanges();
            
            performanceMetrics.recordBatchSuccess(batchId, protectedExposures.size());
            log.info("Risk calculation completed for batch: {}", batchId);

            return Result.success();

        } catch (BatchDataParsingService.BatchDataParsingException e) {
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
        
        batch.failCalculation("Failed to store calculation results: " + error.getMessage());
        batchRepository.save(batch);
        
        unitOfWork.registerEntity(batch);
        unitOfWork.registerEntity(analysis);
        unitOfWork.saveChanges();
        
        performanceMetrics.recordBatchFailure(batch.getId().value(), error.getMessage());
        return Result.failure(error);
    }

    private Result<Void> handleUnexpectedFailure(Batch batch, String batchId, Exception e) {
        if (batch != null) {
            try {
                batch.failCalculation("Calculation failed: " + e.getMessage());
                batchRepository.save(batch);
                unitOfWork.registerEntity(batch);
                unitOfWork.saveChanges();
            } catch (Exception ex) {
                log.error("Failed to mark batch as failed: {}", batchId, ex);
            }
        }

        performanceMetrics.recordBatchFailure(batchId, e.getMessage());
        return Result.failure(ErrorDetail.of("CALCULATION_FAILED", ErrorType.SYSTEM_ERROR,
            "Risk calculation failed: " + e.getMessage(), "calculation.failed"));
    }
}