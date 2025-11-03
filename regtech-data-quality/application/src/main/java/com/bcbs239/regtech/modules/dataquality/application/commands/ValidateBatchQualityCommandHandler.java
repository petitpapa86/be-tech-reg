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
 * Command handler for validating batch quality using the railway pattern.
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
     * Handles the batch quality validation command using the railway pattern.
     * Implements the complete workflow with proper error handling and transaction management.
     */
    @Transactional
    public Result<Void> handle(ValidateBatchQualityCommand command) {
        try {
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
            
            // Railway pattern: clean sequential operations
            var reportResult = createQualityReport(command);
            if (reportResult.isFailure()) return Result.failure(reportResult.getErrors());
            var report = reportResult.getValueOrThrow();
            
            var exposuresResult = downloadExposureData(command);
            if (exposuresResult.isFailure()) {
                handleValidationFailure(report, "Failed to download exposure data");
                return Result.failure(exposuresResult.getErrors());
            }
            var exposures = exposuresResult.getValueOrThrow();
            
            var validationResult = validateQuality(exposures);
            if (validationResult.isFailure()) {
                handleValidationFailure(report, "Quality validation failed");
                return Result.failure(validationResult.getErrors());
            }
            var validation = validationResult.getValueOrThrow();
            
            var recordResult = recordValidationResults(report, validation);
            if (recordResult.isFailure()) {
                handleValidationFailure(report, "Failed to record validation results");
                return recordResult;
            }
            
            var scoresResult = calculateQualityScores(validation);
            if (scoresResult.isFailure()) {
                handleValidationFailure(report, "Failed to calculate quality scores");
                return Result.failure(scoresResult.getErrors());
            }
            var scores = scoresResult.getValueOrThrow();
            
            var scoreRecordResult = recordQualityScores(report, scores);
            if (scoreRecordResult.isFailure()) {
                handleValidationFailure(report, "Failed to record quality scores");
                return scoreRecordResult;
            }
            
            var s3Result = storeDetailedResults(command.batchId(), validation, scores);
            if (s3Result.isFailure()) {
                handleValidationFailure(report, "Failed to store detailed results");
                return Result.failure(s3Result.getErrors());
            }
            var detailsReference = s3Result.getValueOrThrow();
            
            var storeResult = recordS3Reference(report, detailsReference);
            if (storeResult.isFailure()) {
                handleValidationFailure(report, "Failed to record S3 reference");
                return storeResult;
            }
            
            var completeResult = completeValidation(report);
            if (completeResult.isFailure()) {
                handleValidationFailure(report, "Failed to complete validation");
                return completeResult;
            }
            
            var saveResult = saveReport(report);
            if (saveResult.isFailure()) {
                logger.error("Failed to save completed quality report for batch {}", command.batchId().value());
                return Result.failure(saveResult.getErrors());
            }
            
            return finalizeWorkflow(command, scores, detailsReference);
            
        } catch (Exception e) {
            logger.error("Unexpected error during quality validation for batch {}: {}", 
                command.batchId().value(), e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "VALIDATION_UNEXPECTED_ERROR",
                "Unexpected error during quality validation: " + e.getMessage(),
                "validation"
            ));
        }
    }
    
    /**
     * Finalizes the workflow with logging and event publishing.
     */
    private Result<Void> finalizeWorkflow(
        ValidateBatchQualityCommand command, 
        QualityScores scores, 
        S3Reference detailsReference
    ) {
        logger.info("Successfully completed quality validation for batch {} with overall score: {}", 
            command.batchId().value(), scores.overallScore());
        
        // Publish completion event (don't fail the entire operation if this fails)
        publishCompletionEvent(command, scores, detailsReference);
        
        return Result.success();
    }
    
    private Result<QualityReport> createQualityReport(ValidateBatchQualityCommand command) {
        try {
            QualityReport report = QualityReport.createForBatch(command.batchId(), command.bankId());
            
            return report.startValidation()
                .flatMap(ignored -> qualityReportRepository.save(report))
                .map(savedReport -> {
                    logger.debug("Created quality report {} for batch {}", 
                        savedReport.getReportId().value(), command.batchId().value());
                    return savedReport;
                });
            
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
            
            Result<List<ExposureRecord>> downloadResult = command.expectedExposureCount() > 0 
                ? s3StorageService.downloadExposures(command.s3Uri(), command.expectedExposureCount())
                : s3StorageService.downloadExposures(command.s3Uri());
            
            return downloadResult.map(exposures -> {
                logger.info("Successfully downloaded {} exposures for batch {}", 
                    exposures.size(), command.batchId().value());
                return exposures;
            });
            
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
            
            return validationEngine.validateExposures(exposures)
                .map(validation -> {
                    logger.info("Quality validation completed: {}/{} exposures valid, {} total errors", 
                        validation.validExposures(), validation.totalExposures(), validation.allErrors().size());
                    return validation;
                });
            
        } catch (Exception e) {
            logger.error("Failed to validate exposure quality: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "VALIDATION_ENGINE_FAILED",
                "Quality validation engine failed: " + e.getMessage(),
                "validation"
            ));
        }
    }
    
    private Result<Void> recordValidationResults(QualityReport report, ValidationResult validation) {
        return report.recordValidationResults(validation);
    }
    
    private Result<QualityScores> calculateQualityScores(ValidationResult validationResult) {
        try {
            logger.debug("Calculating quality scores from validation results");
            
            return scoringEngine.calculateScores(validationResult)
                .map(scores -> {
                    logger.info("Quality scores calculated: Overall={}, Grade={}", 
                        scores.overallScore(), scores.grade());
                    return scores;
                });
            
        } catch (Exception e) {
            logger.error("Failed to calculate quality scores: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of(
                "SCORING_ENGINE_FAILED",
                "Quality scoring engine failed: " + e.getMessage(),
                "scoring"
            ));
        }
    }
    
    private Result<Void> recordQualityScores(QualityReport report, QualityScores scores) {
        return report.calculateScores(scores);
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
            
            return s3StorageService.storeDetailedResults(batchId, validationResult, metadata)
                .map(reference -> {
                    logger.info("Detailed results stored in S3: {}", reference.uri());
                    return reference;
                });
            
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
    
    private Result<Void> recordS3Reference(QualityReport report, S3Reference detailsReference) {
        return report.storeDetailedResults(detailsReference);
    }
    
    private Result<Void> completeValidation(QualityReport report) {
        return report.completeValidation();
    }
    
    private Result<QualityReport> saveReport(QualityReport report) {
        return qualityReportRepository.save(report)
            .map(savedReport -> {
                logger.debug("Saved quality report {} for batch {}", 
                    savedReport.getReportId().value(), savedReport.getBatchId().value());
                return savedReport;
            });
    }
    
    private Result<Void> publishCompletionEvent(
        ValidateBatchQualityCommand command, 
        QualityScores scores, 
        S3Reference detailsReference
    ) {
        try {
            logger.debug("Publishing batch quality completed event for batch {}", command.batchId().value());
            
            return eventPublisher.publishBatchQualityCompleted(
                command.batchId(),
                command.bankId(),
                scores,
                detailsReference,
                command.correlationId()
            ).map(ignored -> {
                logger.info("Successfully published batch quality completed event for batch {}", 
                    command.batchId().value());
                return null;
            });
            
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