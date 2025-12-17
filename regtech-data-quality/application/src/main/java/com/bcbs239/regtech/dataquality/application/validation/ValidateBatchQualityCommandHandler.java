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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.annotation.Timed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private final int hikariMaxPoolSize;
    private final int configuredMaxInFlight;
    
    public ValidateBatchQualityCommandHandler(
        IQualityReportRepository qualityReportRepository,
        S3StorageService s3StorageService,
        DataQualityRulesService rulesService,
        BaseUnitOfWork unitOfWork,
        @Value("${spring.datasource.hikari.maximum-pool-size:20}") int hikariMaxPoolSize,
        @Value("${dataquality.validation.max-in-flight:0}") int configuredMaxInFlight
    ) {
        this.qualityReportRepository = qualityReportRepository;
        this.s3StorageService = s3StorageService;
        this.rulesService = rulesService;
        this.unitOfWork = unitOfWork;

        this.hikariMaxPoolSize = Math.max(1, hikariMaxPoolSize);
        this.configuredMaxInFlight = Math.max(0, configuredMaxInFlight);
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

        logger.info("Quality validation started: batchId={}, bankId={}",
                command.batchId().value(), command.bankId().value());

        // Check if report already exists and is completed (idempotency)
        Optional<QualityReport> existingReport = qualityReportRepository.findByBatchId(command.batchId());
        if (existingReport.isPresent()) {
            QualityReport existing = existingReport.get();
            if (existing.getStatus().name().equals("COMPLETED") || existing.getStatus().name().equals("FAILED")) {
                logger.info("Quality validation skipped: batchId={}, status={}",
                        command.batchId().value(), existing.getStatus());
                return Result.success();
            }
            logger.debug("Quality report exists but status is {}, will process", existing.getStatus());
        }

        // Create quality report
        QualityReport report = QualityReport.createForBatch(command.batchId(), command.bankId());
        report.startValidation();

        Result<QualityReport> saveResult = qualityReportRepository.save(report);
        if (saveResult.isFailure()) {
            logger.error("Failed to save quality report: {}", saveResult.getError());
            return Result.failure(saveResult.errors());
        }
        report = saveResult.getValueOrThrow();

        // Download exposure data
        logger.debug("Downloading exposure data from S3: {}", command.s3Uri());

        Result<List<ExposureRecord>> exposuresResult = command.expectedExposureCount() > 0
                ? s3StorageService.downloadExposures(command.s3Uri(), command.expectedExposureCount())
                : s3StorageService.downloadExposures(command.s3Uri());

        if (exposuresResult.isFailure()) {
            logger.error("Failed to download exposure data: {}", exposuresResult.getError());
            markReportAsFailed(report, "Failed to download exposure data");
            return Result.failure(exposuresResult.errors());
        }

        List<ExposureRecord> exposures = exposuresResult.getValueOrThrow();
        logger.debug("Downloaded {} exposures", exposures.size());

        // === FIXED: SIMPLE SEQUENTIAL VALIDATION ===
        // This is what made 100k exposures process in 0.992s
        List<ExposureValidationResult> exposureResults = new ArrayList<>(exposures.size());

        long validationStart = System.currentTimeMillis();

        for (ExposureRecord exposure : exposures) {
            ExposureValidationResult result = validateSingleExposure(exposure);
            exposureResults.add(result);
        }

        long validationTime = System.currentTimeMillis() - validationStart;
        logger.debug("Validated {} exposures in {}ms ({} exposures/sec)",
                exposures.size(), validationTime,
                exposures.size() / (validationTime / 1000.0));

        // Convert to map
        Map<String, ExposureValidationResult> resultMap = new HashMap<>(exposureResults.size());
        for (ExposureValidationResult result : exposureResults) {
            resultMap.put(result.exposureId(), result);
        }

        // Batch-level validation
        List<ValidationError> batchErrors = new ArrayList<>();

        // Create ValidationResult
        ValidationResult validation = ValidationResult.fromValidatedExposures(resultMap, batchErrors);

        // Record results
        Result<ValidationResult> validationResult = report.recordValidationAndCalculateScores(validation);
        if (validationResult.isFailure()) {
            logger.error("Failed to record validation: {}", validationResult.getError());
            markReportAsFailed(report, "Failed to record quality validation");
            return Result.failure(validationResult.errors());
        }

        QualityScores scores = report.getScores();

        logger.info("Quality validation completed: batchId={}, valid={}/{}, errors={}, score={}",
                command.batchId().value(),
                validation.validExposures(), validation.totalExposures(),
                validation.allErrors().size(), scores.overallScore());

        // Store results in S3
        Map<String, String> metadata = Map.of(
                "batch-id", command.batchId().value(),
                "overall-score", String.valueOf(scores.overallScore()),
                "grade", scores.grade().name(),
                "compliant", String.valueOf(scores.isCompliant()),
                "total-exposures", String.valueOf(validation.totalExposures()),
                "valid-exposures", String.valueOf(validation.validExposures()),
                "total-errors", String.valueOf(validation.allErrors().size())
        );

        Result<S3Reference> s3Result = s3StorageService.storeDetailedResults(
                command.batchId(), validation, metadata);

        if (s3Result.isFailure()) {
            logger.error("Failed to store results: {}", s3Result.getError());
            markReportAsFailed(report, "Failed to store detailed results");
            return Result.failure(s3Result.errors());
        }

        S3Reference detailsReference = s3Result.getValueOrThrow();

        // Record S3 reference
        Result<Void> storeResult = report.storeDetailedResults(detailsReference);
        if (storeResult.isFailure()) {
            logger.error("Failed to record S3 reference: {}", storeResult.getError());
            markReportAsFailed(report, "Failed to record S3 reference");
            return storeResult;
        }

        // Complete validation
        Result<Void> completeResult = report.completeValidation();
        if (completeResult.isFailure()) {
            logger.error("Failed to complete validation: {}", completeResult.getError());
            markReportAsFailed(report, "Failed to complete validation");
            return completeResult;
        }

        // Save completed report
        Result<QualityReport> finalSaveResult = qualityReportRepository.save(report);
        if (finalSaveResult.isFailure()) {
            logger.error("Failed to save report: {}", finalSaveResult.getError());
            return Result.failure(finalSaveResult.errors());
        }

        // Register with Unit of Work
        unitOfWork.registerEntity(report);
        unitOfWork.saveChanges();

        return Result.success();
    }

    private ExposureValidationResult validateSingleExposure(ExposureRecord exposure) {
        List<ValidationError> errors = rulesService.validateConfigurableRules(exposure);

        // Group errors by dimension
        Map<QualityDimension, List<ValidationError>> dimensionErrors = new HashMap<>();
        for (QualityDimension dimension : QualityDimension.values()) {
            dimensionErrors.put(dimension, new ArrayList<>());
        }
        for (ValidationError error : errors) {
            dimensionErrors.get(error.dimension()).add(error);
        }

        return ExposureValidationResult.builder()
                .exposureId(exposure.exposureId())
                .errors(errors)
                .dimensionErrors(dimensionErrors)
                .isValid(errors.isEmpty())
                .build();
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

