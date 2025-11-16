package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.dataquality.application.integration.S3StorageService;
import com.bcbs239.regtech.dataquality.application.scoring.QualityScoringEngine;
import com.bcbs239.regtech.dataquality.application.integration.events.BatchQualityCompletedEvent;
import com.bcbs239.regtech.dataquality.application.integration.events.BatchQualityFailedEvent;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import java.util.Map;
import java.util.HashMap;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
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
    private final IIntegrationEventBus eventBus;
    
    public ValidateBatchQualityCommandHandler(
        IQualityReportRepository qualityReportRepository,
        QualityValidationEngine validationEngine,
        QualityScoringEngine scoringEngine,
        S3StorageService s3StorageService,
        IIntegrationEventBus eventBus
    ) {
        this.qualityReportRepository = qualityReportRepository;
        this.validationEngine = validationEngine;
        this.scoringEngine = scoringEngine;
        this.s3StorageService = s3StorageService;
        this.eventBus = eventBus;
    }
    
    /**
     * Handles the batch quality validation command using the railway pattern.
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
            return Result.success();
        }
        
        // Create quality report
        QualityReport report = QualityReport.createForBatch(command.batchId(), command.bankId());

        QualityReport finalReport = report;
        Result<QualityReport> reportResult = report.startValidation()
            .flatMap(ignored -> qualityReportRepository.save(finalReport))
            .map(savedReport -> {
                logger.debug("Created quality report {} for batch {}", 
                    savedReport.getReportId().value(), command.batchId().value());
                return savedReport;
            });
        
        if (reportResult.isFailure()) {
            return Result.failure(reportResult.errors());
        }
        report = reportResult.getValueOrThrow();
        
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
        
        // Validate quality
        logger.debug("Starting quality validation for {} exposures", exposures.size());
        
        Result<ValidationResult> validationResult = validationEngine.validateExposures(exposures)
            .map(validation -> {
                logger.info("Quality validation completed: {}/{} exposures valid, {} total errors", 
                    validation.validExposures(), validation.totalExposures(), validation.allErrors().size());
                return validation;
            });
        
        if (validationResult.isFailure()) {
            logger.error("Failed to validate exposure quality: {}", 
                validationResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Quality validation failed");
            return Result.failure(validationResult.errors());
        }
        ValidationResult validation = validationResult.getValueOrThrow();
        
        // Record validation results
        Result<Void> recordResult = report.recordValidationResults(validation);
        if (recordResult.isFailure()) {
            logger.error("Failed to record validation results: {}", 
                recordResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Failed to record validation results");
            return recordResult;
        }
        
        // Calculate quality scores
        logger.debug("Calculating quality scores from validation results");
        
        Result<QualityScores> scoresResult = scoringEngine.calculateScores(validation)
            .map(scores -> {
                logger.info("Quality scores calculated: Overall={}, Grade={}", 
                    scores.overallScore(), scores.grade());
                return scores;
            });
        
        if (scoresResult.isFailure()) {
            logger.error("Failed to calculate quality scores: {}", 
                scoresResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Failed to calculate quality scores");
            return Result.failure(scoresResult.errors());
        }
        QualityScores scores = scoresResult.getValueOrThrow();
        
        // Record quality scores
        Result<Void> scoreRecordResult = report.calculateScores(scores);
        if (scoreRecordResult.isFailure()) {
            logger.error("Failed to record quality scores: {}", 
                scoreRecordResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
            markReportAsFailed(report, "Failed to record quality scores");
            return scoreRecordResult;
        }
        
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
        Result<QualityReport> saveResult = qualityReportRepository.save(report)
            .map(savedReport -> {
                logger.debug("Saved quality report {} for batch {}", 
                    savedReport.getReportId().value(), savedReport.getBatchId().value());
                return savedReport;
            });
        
        if (saveResult.isFailure()) {
            logger.error("Failed to save completed quality report for batch {}", command.batchId().value());
            return Result.failure(saveResult.errors());
        }
        
        // Finalize workflow with logging and event publishing
        logger.info("Successfully completed quality validation for batch {} with overall score: {}", 
            command.batchId().value(), scores.overallScore());
        
        // Publish completion event (best effort - don't fail if this fails)
        publishCompletionEvent(command, scores, detailsReference);
        
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
        
        publishFailureEvent(report, errorMessage);
    }
    
    private void publishCompletionEvent(ValidateBatchQualityCommand command, QualityScores scores, S3Reference detailsReference) {
        logger.debug("Publishing batch quality completed event for batch {}", command.batchId().value());

        Map<String, Object> validationSummary = new HashMap<>();
        Map<String, Object> processingMetadata = new HashMap<>();
        if (command.correlationId() != null) {
            processingMetadata.put("correlationId", command.correlationId());
        }

        BatchQualityCompletedEvent event = new BatchQualityCompletedEvent(
            command.batchId(),
            command.bankId(),
            scores,
            detailsReference,
            validationSummary,
            processingMetadata
        );

        eventBus.publish(event);
        logger.info("Successfully published batch quality completed event for batch {}", command.batchId().value());
    }
    
    private void publishFailureEvent(QualityReport report, String errorMessage) {
        Map<String, Object> errorDetails = new HashMap<>();
        Map<String, Object> processingMetadata = new HashMap<>();

        BatchQualityFailedEvent event = new BatchQualityFailedEvent(
            report.getBatchId(),
            report.getBankId(),
            errorMessage,
            errorDetails,
            processingMetadata
        );

        eventBus.publish(event);
        logger.info("Published batch quality failed event for batch {}", report.getBatchId().value());
    }
}

