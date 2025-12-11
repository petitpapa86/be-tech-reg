package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.integration.S3StorageService;
import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.annotation.Timed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Command handler for validating batch quality using proper DDD approach.
 * 
 * <p>This handler delegates business logic to the aggregate root and value object factories:
 * <ul>
 *   <li>{@link QualityReport#executeQualityValidation} - Aggregate orchestrates validation workflow</li>
 *   <li>{@link ValidationResult#validate} - Value object factory applies specifications</li>
 *   <li>{@link QualityScores#calculateFrom} - Value object factory calculates scores</li>
 *   <li>Application layer only handles: transactions, infrastructure (S3, events), and persistence</li>
 * </ul>
 * 
 * <p>Application layer responsibilities:</p>
 * <ol>
 *   <li>Transaction management (@Transactional)</li>
 *   <li>Downloading data from S3 (infrastructure)</li>
 *   <li>Calling aggregate business methods</li>
 *   <li>Storing results in S3 (infrastructure)</li>
 *   <li>Publishing integration events (infrastructure)</li>
 *   <li>Saving aggregates to repository</li>
 * </ol>
 * 
 * <p>This is proper DDD - business logic lives in aggregates and value objects, application layer only coordinates infrastructure.</p>
 */
@Component
public class ValidateBatchQualityCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidateBatchQualityCommandHandler.class);
    
    private final IQualityReportRepository qualityReportRepository;
    private final S3StorageService s3StorageService;
    private final DataQualityRulesService rulesService;
    private final BaseUnitOfWork unitOfWork;
    
    public ValidateBatchQualityCommandHandler(
        IQualityReportRepository qualityReportRepository,
        S3StorageService s3StorageService,
        DataQualityRulesService rulesService,
        BaseUnitOfWork unitOfWork
    ) {
        this.qualityReportRepository = qualityReportRepository;
        this.s3StorageService = s3StorageService;
        this.rulesService = rulesService;
        this.unitOfWork = unitOfWork;
    }
    
    /**
     * Handles the batch quality validation command using the railway pattern.
     * Implements the complete workflow with proper error handling and transaction management.
     */
    @Transactional
    @Timed(value = "dataquality.validation.batch", description = "Time taken to validate batch quality")
    public Result<Void> handle(ValidateBatchQualityCommand command) {
        // Validate command parameters
        command.validate();
        
        logger.info("Starting quality validation for batch {} from bank {}", 
            command.batchId().value(), command.bankId().value());
        
        // Check if report already exists and is completed (idempotency)
        Optional<QualityReport> existingReport = qualityReportRepository.findByBatchId(command.batchId());
        if (existingReport.isPresent()) {
            QualityReport existing = existingReport.get();
            // Only skip if already completed or failed - let in-progress reports continue
            if (existing.getStatus().name().equals("COMPLETED") || existing.getStatus().name().equals("FAILED")) {
                logger.info("Quality report already {} for batch {}, skipping", 
                    existing.getStatus(), command.batchId().value());
                return Result.success();
            }
            logger.debug("Quality report exists but status is {}, will process", existing.getStatus());
        }
        
        // Try to create and save quality report (may fail if another thread created it)
        QualityReport report;
        try {
            report = QualityReport.createForBatch(command.batchId(), command.bankId());
            report.startValidation();
            
            Result<QualityReport> saveResult = qualityReportRepository.save(report);
            if (saveResult.isFailure()) {
                // If duplicate key error, another thread is processing it
                if (saveResult.getError().map(e -> 
                    e.getCode().equals("QUALITY_REPORT_DUPLICATE_BATCH_ID")
                ).orElse(false)) {
                    logger.debug("Concurrent creation detected for batch {}, another thread created the report", command.batchId().value());
                    return Result.success();
                }
                logger.error("Failed to save quality report for batch {}: {}", 
                    command.batchId().value(), 
                    saveResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return Result.failure(saveResult.errors());
            }
            report = saveResult.getValueOrThrow();
        } catch (Exception e) {
            logger.warn("Exception during initial quality report creation for batch {}: {}", 
                command.batchId().value(), e.getMessage());
            // Check if report was created by another thread despite the exception
            Optional<QualityReport> retryCheck = qualityReportRepository.findByBatchId(command.batchId());
            if (retryCheck.isPresent()) {
                logger.debug("Report exists after exception, another thread succeeded for batch {}", command.batchId().value());
                return Result.success();
            }
            throw e;
        }
        
        // Download exposure data
        logger.debug("Downloading exposure data from S3: {}", command.s3Uri());
        
        Result<List<ExposureRecord>> downloadResult = command.expectedExposureCount() > 0 
            ? s3StorageService.downloadExposures(command.s3Uri(), command.expectedExposureCount())
            : s3StorageService.downloadExposures(command.s3Uri());
        
        Result<List<ExposureRecord>> exposuresResult = downloadResult.map(exposures -> {
            logger.info("Successfully downloaded {} exposures for batch {}", 
                exposures.size(), command.batchId().value());
            return exposures;
        });
        
        if (exposuresResult.isFailure()) {
            logger.error("Failed to download exposure data for batch {}: {}", 
                command.batchId().value(), exposuresResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Failed to download exposure data");
            return Result.failure(exposuresResult.errors());
        }
        List<ExposureRecord> exposures = exposuresResult.getValueOrThrow();
        
        // Execute quality validation using Rules Engine (application layer responsibility)
        logger.debug("Executing quality validation for {} exposures using Rules Engine", exposures.size());
        
        // Application layer orchestrates infrastructure (Rules Engine)
        // Validate each exposure using the Rules Engine
        Map<String, ExposureValidationResult> exposureResults = new HashMap<>();
        for (ExposureRecord exposure : exposures) {
            List<ValidationError> errors = rulesService.validateConfigurableRules(exposure);
            
            // Group errors by dimension
            Map<QualityDimension, List<ValidationError>> dimensionErrors = new HashMap<>();
            for (QualityDimension dimension : QualityDimension.values()) {
                dimensionErrors.put(dimension, new ArrayList<>());
            }
            for (ValidationError error : errors) {
                dimensionErrors.get(error.dimension()).add(error);
            }
            
            ExposureValidationResult result = ExposureValidationResult.builder()
                .exposureId(exposure.exposureId())
                .errors(errors)
                .dimensionErrors(dimensionErrors)
                .isValid(errors.isEmpty())
                .build();
            
            exposureResults.put(exposure.exposureId(), result);
        }
        
        // Batch-level validation (if needed in the future)
        List<ValidationError> batchErrors = new ArrayList<>();
        
        // Create ValidationResult from the validated exposures
        ValidationResult validation = ValidationResult.fromValidatedExposures(exposureResults, batchErrors);
        
        // Tell the aggregate to record the results (domain logic)
        Result<ValidationResult> validationResult = report.recordValidationAndCalculateScores(validation);
        
        if (validationResult.isFailure()) {
            logger.error("Failed to record quality validation: {}", 
                validationResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Failed to record quality validation");
            return Result.failure(validationResult.errors());
        }
        
        QualityScores scores = report.getScores(); // Scores calculated by aggregate
        
        logger.info("Quality validation completed: {}/{} exposures valid, {} total errors, Overall score: {}", 
            validation.validExposures(), validation.totalExposures(), validation.allErrors().size(),
            scores.overallScore());
        
        // Store detailed results in S3
        logger.debug("Storing detailed validation results in S3 for batch {}", command.batchId().value());
        
        java.util.Map<String, String> metadata = java.util.Map.of(
            "batch-id", command.batchId().value(),
            "overall-score", String.valueOf(scores.overallScore()),
            "grade", scores.grade().name(),
            "compliant", String.valueOf(scores.isCompliant()),
            "total-exposures", String.valueOf(validation.totalExposures()),
            "valid-exposures", String.valueOf(validation.validExposures()),
            "total-errors", String.valueOf(validation.allErrors().size())
        );
        
        Result<S3Reference> s3Result = s3StorageService.storeDetailedResults(command.batchId(), validation, metadata)
            .map(reference -> {
                logger.info("Detailed results stored in S3: {}", reference.uri());
                return reference;
            });
        
        if (s3Result.isFailure()) {
            logger.error("Failed to store detailed results for batch {}: {}", 
                command.batchId().value(), s3Result.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Failed to store detailed results");
            return Result.failure(s3Result.errors());
        }
        S3Reference detailsReference = s3Result.getValueOrThrow();
        
        // Record S3 reference
        Result<Void> storeResult = report.storeDetailedResults(detailsReference);
        if (storeResult.isFailure()) {
            logger.error("Failed to record S3 reference: {}", 
                storeResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Failed to record S3 reference");
            return storeResult;
        }
        
        // Complete validation
        Result<Void> completeResult = report.completeValidation();
        if (completeResult.isFailure()) {
            logger.error("Failed to complete validation: {}", 
                completeResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Failed to complete validation");
            return completeResult;
        }
        
        // Save completed report
        Result<QualityReport> finalSaveResult = qualityReportRepository.save(report)
            .map(savedReport -> {
                logger.debug("Saved quality report {} for batch {}", 
                    savedReport.getReportId().value(), savedReport.getBatchId().value());
                return savedReport;
            });
        
        if (finalSaveResult.isFailure()) {
            logger.error("Failed to save completed quality report for batch {}", command.batchId().value());
            return Result.failure(finalSaveResult.errors());
        }
        
        // Register aggregate with Unit of Work to persist domain events
        unitOfWork.registerEntity(report);
        unitOfWork.saveChanges();
        
        // Finalize workflow with logging
        logger.info("Successfully completed quality validation for batch {} with overall score: {}", 
            command.batchId().value(), scores.overallScore());
        
        return Result.success();
    }
    
    private void markReportAsFailed(QualityReport report, String errorMessage) {
        Result<Void> failResult = report.markAsFailed(errorMessage);
        if (failResult.isFailure()) {
            logger.error("Failed to mark report as failed: {}", 
                failResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
        }
        
        Result<QualityReport> saveResult = qualityReportRepository.save(report);
        if (saveResult.isFailure()) {
            logger.error("Failed to save failed report: {}", 
                saveResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
        }
        
        // Register failed aggregate with Unit of Work
        unitOfWork.registerEntity(report);
        unitOfWork.saveChanges();
    }
}

