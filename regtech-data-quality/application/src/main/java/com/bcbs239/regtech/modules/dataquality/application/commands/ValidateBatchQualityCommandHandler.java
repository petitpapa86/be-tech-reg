package com.bcbs239.regtech.modules.dataquality.application.commands;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.modules.dataquality.application.services.*;
import com.bcbs239.regtech.modules.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.modules.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.modules.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ValidationResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Command handler for validating batch quality.
 * Orchestrates the complete quality validation workflow including:
 * 1. Creating quality report
 * 2. Downloading exposure data from S3
 * 3. Validating quality across six dimensions
 * 4. Calculating quality scores
 * 5. Storing detailed results in S3
 * 6. Publishing completion events
 */
@Component
public class ValidateBatchQualityCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidateBatchQualityCommandHandler.class);
    
    private final IQualityReportRepository qualityReportRepository;
    private final QualityValidationEngine validationEngine;
    private final QualityScoringEngine scoringEngine;
    private final S3StorageService s3StorageService;
    private final CrossModuleEventPublisher eventPublisher;
    
    public ValidateBatchQualityCommandHandler(
        IQualityReportRepository qualityReportRepository,
        QualityValidationEngine validationEngine,
        QualityScoringEngine scoringEngine,
        S3StorageService s3StorageService,
        CrossModuleEventPublisher eventPublisher
    ) {
        this.qualityReportRepository = qualityReportRepository;
        this.validationEngine = validationEngine;
        this.scoringEngine = scoringEngine;
        this.s3StorageService = s3StorageService;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Handles the batch quality validation command.
     * Implements the complete workflow with proper error handling and transaction management.
     */
    @Transactional
    public Result<Void> handle(ValidateBatchQualityCommand command) {
            // Validate command parameters
            command.validate();
            
            logger.info("Starting quality validation for batch {} from bank {}", 
                command.batchId().value(), command.bankId().value());
            
            // Check for idempotency - if report already exists, skip processing
            if (qualityReportRepository.existsByBatchId(command.batchId())) {
                logger.info("Quality report already exists for batch {}, skipping processing", 
                    command.batchId().value());
                return Result.success();
            }
            
            // Step 1: Create quality report
            Result<QualityReport> reportResult = createQualityReport(command);
            if (reportResult.isFailure()) {
                return Result.failure(reportResult.getErrors());
            }
            QualityReport report = reportResult.getValueOrThrow();
            
            // Step 2: Download and parse exposure data
            Result<List<ExposureRecord>> downloadResult = downloadExposureData(command);
            if (downloadResult.isFailure()) {
                handleValidationFailure(report, "Failed to download exposure data: " + 
                    downloadResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return Result.failure(downloadResult.getErrors());
            }
            List<ExposureRecord> exposures = downloadResult.getValueOrThrow();
            
            // Step 3: Validate quality across all dimensions
            Result<ValidationResult> validationResult = validateQuality(exposures);
            if (validationResult.isFailure()) {
                handleValidationFailure(report, "Quality validation failed: " + 
                    validationResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return Result.failure(validationResult.getErrors());
            }
            ValidationResult validation = validationResult.getValueOrThrow();
            
            // Step 4: Record validation results
            Result<Void> recordResult = report.recordValidationResults(validation);
            if (recordResult.isFailure()) {
                handleValidationFailure(report, "Failed to record validation results: " + 
                    recordResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return recordResult;
            }
            
            // Step 5: Calculate quality scores
            Result<QualityScores> scoresResult = calculateQualityScores(validation);
            if (scoresResult.isFailure()) {
                handleValidationFailure(report, "Failed to calculate quality scores: " + 
                    scoresResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return Result.failure(scoresResult.getErrors());
            }
            QualityScores scores = scoresResult.getValueOrThrow();
            
            Result<Void> scoreRecordResult = report.calculateScores(scores);
            if (scoreRecordResult.isFailure()) {
                handleValidationFailure(report, "Failed to record quality scores: " + 
                    scoreRecordResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return scoreRecordResult;
            }
            
            // Step 6: Store detailed results in S3
            Result<S3Reference> s3Result = storeDetailedResults(command.batchId(), validation, scores);
            if (s3Result.isFailure()) {
                handleValidationFailure(report, "Failed to store detailed results: " + 
                    s3Result.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return Result.failure(s3Result.getErrors());
            }
            S3Reference detailsReference = s3Result.getValueOrThrow();
            
            Result<Void> storeResult = report.storeDetailedResults(detailsReference);
            if (storeResult.isFailure()) {
                handleValidationFailure(report, "Failed to record S3 reference: " + 
                    storeResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return storeResult;
            }
            
            // Step 7: Complete validation
            Result<Void> completeResult = report.completeValidation();
            if (completeResult.isFailure()) {
                handleValidationFailure(report, "Failed to complete validation: " + 
                    completeResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return completeResult;
            }
            
            // Step 8: Save final report state
            Result<QualityReport> saveResult = qualityReportRepository.save(report);
            if (saveResult.isFailure()) {
                logger.error("Failed to save completed quality report for batch {}: {}", 
                    command.batchId().value(), saveResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return Result.failure(saveResult.getErrors());
            }
            
            // Step 9: Publish completion event
            Result<Void> publishResult = publishCompletionEvent(command, scores, detailsReference);
            if (publishResult.isFailure()) {
                logger.warn("Failed to publish completion event for batch {}: {}", 
                    command.batchId().value(), publishResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                // Don't fail the entire operation if event publishing fails
            }
            
            logger.info("Successfully completed quality validation for batch {} with overall score: {}", 
                command.batchId().value(), scores.overallScore());
            
            return Result.success();
       
    }
    
    private Result<QualityReport> createQualityReport(ValidateBatchQualityCommand command) {
        try {
            QualityReport report = QualityReport.createForBatch(command.batchId(), command.bankId());
            
            Result<Void> startResult = report.startValidation();
            if (startResult.isFailure()) {
                return Result.failure(startResult.getErrors());
            }
            
            Result<QualityReport> saveResult = qualityReportRepository.save(report);
            if (saveResult.isFailure()) {
                logger.error("Failed to save initial quality report for batch {}: {}", 
                    command.batchId().value(), saveResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                return saveResult;
            }
            
            logger.debug("Created quality report {} for batch {}", 
                report.getReportId().value(), command.batchId().value());
            
            return saveResult;
            
        } catch (Exception e) {
            logger.error("Failed to create quality report for batch {}: {}", 
                command.batchId().value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "REPORT_CREATION_FAILED",
                "Failed to create quality report: " + e.getMessage(),
                "report"
            ));
        }
    }
    
    private Result<List<ExposureRecord>> downloadExposureData(ValidateBatchQualityCommand command) {
        try {
            logger.debug("Downloading exposure data from S3: {}", command.s3Uri());
            
            Result<List<ExposureRecord>> downloadResult;
            if (command.expectedExposureCount() > 0) {
                downloadResult = s3StorageService.downloadExposures(command.s3Uri(), command.expectedExposureCount());
            } else {
                downloadResult = s3StorageService.downloadExposures(command.s3Uri());
            }
            
            if (downloadResult.isSuccess()) {
                List<ExposureRecord> exposures = downloadResult.getValueOrThrow();
                logger.info("Successfully downloaded {} exposures for batch {}", 
                    exposures.size(), command.batchId().value());
            }
            
            return downloadResult;
            
        } catch (Exception e) {
            logger.error("Failed to download exposure data for batch {}: {}", 
                command.batchId().value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_DOWNLOAD_FAILED",
                "Failed to download exposure data: " + e.getMessage(),
                "s3Download"
            ));
        }
    }
    
    private Result<ValidationResult> validateQuality(List<ExposureRecord> exposures) {
        try {
            logger.debug("Starting quality validation for {} exposures", exposures.size());
            
            Result<ValidationResult> validationResult = validationEngine.validateExposures(exposures);
            
            if (validationResult.isSuccess()) {
                ValidationResult validation = validationResult.getValueOrThrow();
                logger.info("Quality validation completed: {}/{} exposures valid, {} total errors", 
                    validation.validExposures(), validation.totalExposures(), validation.allErrors().size());
            }
            
            return validationResult;
            
        } catch (Exception e) {
            logger.error("Failed to validate exposure quality: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "VALIDATION_ENGINE_FAILED",
                "Quality validation engine failed: " + e.getMessage(),
                "validation"
            ));
        }
    }
    
    private Result<QualityScores> calculateQualityScores(ValidationResult validationResult) {
        try {
            logger.debug("Calculating quality scores from validation results");
            
            Result<QualityScores> scoresResult = scoringEngine.calculateScores(validationResult);
            
            if (scoresResult.isSuccess()) {
                QualityScores scores = scoresResult.getValueOrThrow();
                logger.info("Quality scores calculated: Overall={}, Grade={}", 
                    scores.overallScore(), scores.grade());
            }
            
            return scoresResult;
            
        } catch (Exception e) {
            logger.error("Failed to calculate quality scores: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "SCORING_ENGINE_FAILED",
                "Quality scoring engine failed: " + e.getMessage(),
                "scoring"
            ));
        }
    }
    
    private Result<S3Reference> storeDetailedResults(
        com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId batchId, 
        ValidationResult validationResult,
        QualityScores scores
    ) {
        try {
            logger.debug("Storing detailed validation results in S3 for batch {}", batchId.value());
            
            // Add metadata for the S3 object
            java.util.Map<String, String> metadata = java.util.Map.of(
                "batch-id", batchId.value(),
                "overall-score", String.valueOf(scores.overallScore()),
                "grade", scores.grade().name(),
                "compliant", String.valueOf(scores.isCompliant()),
                "total-exposures", String.valueOf(validationResult.totalExposures()),
                "valid-exposures", String.valueOf(validationResult.validExposures()),
                "total-errors", String.valueOf(validationResult.allErrors().size())
            );
            
            Result<S3Reference> s3Result = s3StorageService.storeDetailedResults(
                batchId, validationResult, metadata);
            
            if (s3Result.isSuccess()) {
                S3Reference reference = s3Result.getValueOrThrow();
                logger.info("Detailed results stored in S3: {}", reference.uri());
            }
            
            return s3Result;
            
        } catch (Exception e) {
            logger.error("Failed to store detailed results for batch {}: {}", 
                batchId.value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "S3_STORAGE_FAILED",
                "Failed to store detailed results: " + e.getMessage(),
                "s3Storage"
            ));
        }
    }
    
    private Result<Void> publishCompletionEvent(
        ValidateBatchQualityCommand command, 
        QualityScores scores, 
        S3Reference detailsReference
    ) {
        try {
            logger.debug("Publishing batch quality completed event for batch {}", command.batchId().value());
            
            Result<Void> publishResult = eventPublisher.publishBatchQualityCompleted(
                command.batchId(),
                command.bankId(),
                scores,
                detailsReference,
                command.correlationId()
            );
            
            if (publishResult.isSuccess()) {
                logger.info("Successfully published batch quality completed event for batch {}", 
                    command.batchId().value());
            }
            
            return publishResult;
            
        } catch (Exception e) {
            logger.error("Failed to publish completion event for batch {}: {}", 
                command.batchId().value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISHING_FAILED",
                "Failed to publish completion event: " + e.getMessage(),
                "eventPublishing"
            ));
        }
    }
    
    private void handleValidationFailure(QualityReport report, String errorMessage) {
        try {
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
            
            // Attempt to publish failure event
            Result<Void> publishResult = eventPublisher.publishBatchQualityFailed(
                report.getBatchId(),
                report.getBankId(),
                errorMessage
            );
            if (publishResult.isFailure()) {
                logger.warn("Failed to publish failure event: {}", 
                    publishResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            }
            
        } catch (Exception e) {
            logger.error("Error while handling validation failure: {}", e.getMessage(), e);
        }
    }
}