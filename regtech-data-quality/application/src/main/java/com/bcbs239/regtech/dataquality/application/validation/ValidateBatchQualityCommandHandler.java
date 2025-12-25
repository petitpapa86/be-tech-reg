package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ValidateBatchQualityCommandHandler {

    private static final Logger logger = LoggerFactory.getLogger(ValidateBatchQualityCommandHandler.class);

    private final IQualityReportRepository qualityReportRepository;
    private final S3StorageService s3StorageService;
    private final DataQualityRulesService rulesService;
    private final ParallelExposureValidationCoordinator coordinator;
    private final BaseUnitOfWork unitOfWork;

    public ValidateBatchQualityCommandHandler(
            IQualityReportRepository qualityReportRepository,
            S3StorageService s3StorageService,
            DataQualityRulesService rulesService,
            ParallelExposureValidationCoordinator coordinator,
            BaseUnitOfWork unitOfWork
    ) {
        this.qualityReportRepository = qualityReportRepository;
        this.s3StorageService = s3StorageService;
        this.rulesService = rulesService;
        this.coordinator = coordinator;
        this.unitOfWork = unitOfWork;
    }

    @Timed(value = "dataquality.validation.batch", description = "Time taken to validate batch quality")
    public Result<Void> handle(ValidateBatchQualityCommand command) {
        command.validate();

        Optional<QualityReport> existingReport = qualityReportRepository.findByBatchId(command.batchId());
        if (existingReport.isPresent()) {
            QualityReport existing = existingReport.get();
            if (existing.getStatus().name().equals("COMPLETED") || existing.getStatus().name().equals("FAILED")) {
                return Result.success();
            }
        }

        QualityReport report;
        try {
            report = QualityReport.createForBatch(command.batchId(), command.bankId());
            report.startValidation(CorrelationContext.correlationId());

            Result<QualityReport> saveResult = qualityReportRepository.save(report);
            if (saveResult.isFailure()) {
                // If duplicate key error, another thread is processing it
                if (saveResult.getError().map(e ->
                        e.getCode().equals("QUALITY_REPORT_DUPLICATE_BATCH_ID")
                ).orElse(false)) {
                    return Result.success();
                }

                return Result.failure(saveResult.errors());
            }
            report = saveResult.getValueOrThrow();
        } catch (Exception e) {
            // Check if report was created by another thread despite the exception
            Optional<QualityReport> retryCheck = qualityReportRepository.findByBatchId(command.batchId());
            if (retryCheck.isPresent()) {
                return Result.success();
            }

            logger.warn("Exception during initial quality report creation for batchId={}: {}",
                    command.batchId().value(), e.getMessage());
            throw e;
        }

        Result<List<ExposureRecord>> downloadResult = command.expectedExposureCount() > 0
                ? s3StorageService.downloadExposures(command.s3Uri(), command.expectedExposureCount())
                : s3StorageService.downloadExposures(command.s3Uri());

        Result<List<ExposureRecord>> exposuresResult = downloadResult.map(exposures -> {
            logger.debug("Downloaded {} exposures for batchId={}",
                    exposures.size(), command.batchId().value());
            return exposures;
        });

        if (exposuresResult.isFailure()) {
            markReportAsFailed(report, "Failed to download exposure data", CorrelationContext.correlationId());
            return Result.failure(exposuresResult.errors());
        }
        List<ExposureRecord> exposures = exposuresResult.getValueOrThrow();

        ValidationBatchResult batchResult = coordinator.validateAll(exposures, rulesService);

        List<ValidationResults> allResults = batchResult.results();
        Map<String, ExposureValidationResult> exposureResults = batchResult.exposureResults();

        rulesService.batchPersistValidationResults(command.batchId().value(), allResults);

        // Batch-level validation (if needed in the future)
        List<ValidationError> batchErrors = new ArrayList<>();

        // Create ValidationResult from the validated exposures
        ValidationResult validation = ValidationResult.fromValidatedExposures(exposureResults, batchErrors);

        // Tell the aggregate to record the results (domain logic)
        Result<ValidationResult> validationResult = report.recordValidationAndCalculateScores(validation, CorrelationContext.correlationId());
        if (validationResult.isFailure()) {
            markReportAsFailed(report, "Failed to record validation results", CorrelationContext.correlationId());
            return Result.failure(validationResult.errors());
        }

        QualityScores scores = report.getScores(); // Scores calculated by aggregate

        logger.info("Quality validation completed: batchId={}, validExposures={}/{}, totalErrors={}, overallScore={}",
                command.batchId().value(),
                validation.validExposures(), validation.totalExposures(), validation.allErrors().size(),
                scores.overallScore());

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
                    logger.debug("Detailed results stored in S3: {}", reference.uri());
                    return reference;
                });

        if (s3Result.isFailure()) {
            markReportAsFailed(report, "Failed to store detailed results", CorrelationContext.correlationId());
            return Result.failure(s3Result.errors());
        }
        S3Reference detailsReference = s3Result.getValueOrThrow();

        // Record S3 reference
        Result<Void> storeResult = report.storeDetailedResults(detailsReference);
        if (storeResult.isFailure()) {
            markReportAsFailed(report, "Failed to record S3 reference", CorrelationContext.correlationId());
            return storeResult;
        }

        // Complete validation
        Result<Void> completeResult = report.completeValidation();
        if (completeResult.isFailure()) {
            markReportAsFailed(report, "Failed to complete validation", CorrelationContext.correlationId());
            return completeResult;
        }

        Result<QualityReport> finalSaveResult = qualityReportRepository.save(report)
                .map(savedReport -> savedReport);

        if (finalSaveResult.isFailure()) {
            return Result.failure(finalSaveResult.errors());
        }

        unitOfWork.registerEntity(report);
        unitOfWork.saveChanges();

        return Result.success();
    }

    private void markReportAsFailed(QualityReport report, String errorMessage, String correlationId) {
        report.markAsFailed(errorMessage, correlationId);

        qualityReportRepository.save(report);

        unitOfWork.registerEntity(report);
        unitOfWork.saveChanges();
    }
}

