package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.integration.S3StorageService;
import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.application.rulesengine.ValidationResultsDto;
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
            // Only skip if already completed or failed - let in-progress reports continue
            if (existing.getStatus().name().equals("COMPLETED") || existing.getStatus().name().equals("FAILED")) {
                logger.info("Quality validation skipped: batchId={}, status={}",
                    command.batchId().value(), existing.getStatus());
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
            // Check if report was created by another thread despite the exception
            Optional<QualityReport> retryCheck = qualityReportRepository.findByBatchId(command.batchId());
            if (retryCheck.isPresent()) {
                logger.debug("Quality report exists after exception during creation; assuming concurrent creation for batchId={}",
                    command.batchId().value());
                return Result.success();
            }

            logger.warn("Exception during initial quality report creation for batchId={}: {}",
                command.batchId().value(), e.getMessage());
            throw e;
        }

        // Download exposure data
        logger.debug("Downloading exposure data from S3: {}", command.s3Uri());

        Result<List<ExposureRecord>> downloadResult = command.expectedExposureCount() > 0
            ? s3StorageService.downloadExposures(command.s3Uri(), command.expectedExposureCount())
            : s3StorageService.downloadExposures(command.s3Uri());

        Result<List<ExposureRecord>> exposuresResult = downloadResult.map(exposures -> {
            logger.debug("Downloaded {} exposures for batchId={}",
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
        Map<String, ExposureValidationResult> exposureResults = new ConcurrentHashMap<>(Math.max(16, exposures.size()));
        List<ValidationResultsDto> allValidationResults = new ArrayList<>(exposures.size());

        // For large batches, run exposure validations concurrently.
        // Worker tasks perform rule execution + mapping only; persistence is done once after all validations complete.
        int cpuBoundMax = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() * 2, 32));
        int requestedMaxInFlight = (configuredMaxInFlight > 0) ? configuredMaxInFlight : cpuBoundMax;
        int reservedConnections = 2; // leave room for the handler TX + misc repository calls
        int connectionBudgetForWorkers = Math.max(1, hikariMaxPoolSize - reservedConnections);
        int safeMaxInFlightFromPool = Math.max(1, connectionBudgetForWorkers);
        int maxInFlight = Math.max(1, Math.min(requestedMaxInFlight, safeMaxInFlightFromPool));
        boolean useParallel = exposures.size() >= Math.max(1_000, maxInFlight * 4);

        if (!useParallel) {
            for (ExposureRecord exposure : exposures) {
                ValidationResultsDto validationResults = validateSingleExposureNoPersist(exposure);
                allValidationResults.add(validationResults);

                ExposureValidationResult result = createExposureValidationResult(validationResults);
                exposureResults.put(exposure.exposureId(), result);
            }
        } else {
            logger.debug(
                "Validating {} exposures in parallel (virtual threads), maxInFlight={} (hikariMaxPoolSize={}, configuredMaxInFlight={})",
                exposures.size(),
                maxInFlight,
                hikariMaxPoolSize,
                configuredMaxInFlight
            );

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var completion = new ExecutorCompletionService<ValidationResultsDto>(executor);

                int submitted = 0;
                int completed = 0;
                var iterator = exposures.iterator();

                // Prime the pump with up to maxInFlight tasks
                while (submitted - completed < maxInFlight && iterator.hasNext()) {
                    ExposureRecord exposure = iterator.next();
                    completion.submit(() -> validateSingleExposureNoPersist(exposure));
                    submitted++;
                }

                while (completed < exposures.size()) {
                    Future<ValidationResultsDto> future = completion.take();
                    ValidationResultsDto validationResults = future.get();
                    allValidationResults.add(validationResults);

                    ExposureValidationResult result = createExposureValidationResult(validationResults);
                    exposureResults.put(result.exposureId(), result);
                    completed++;

                    // Keep a bounded number of tasks in flight
                    if (iterator.hasNext()) {
                        ExposureRecord next = iterator.next();
                        completion.submit(() -> validateSingleExposureNoPersist(next));
                        submitted++;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Batch quality validation interrupted for batch {}", command.batchId().value(), e);
                markReportAsFailed(report, "Quality validation interrupted");
                return Result.failure(
                    ErrorDetail.of(
                        "DATA_QUALITY_VALIDATION_INTERRUPTED",
                        com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR,
                        "Quality validation interrupted",
                        "dataquality.validation.interrupted"
                    )
                );
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause == null) {
                    cause = e;
                }
                logger.error("Batch quality validation failed for batch {}: {}", command.batchId().value(), cause.getMessage(), cause);
                markReportAsFailed(report, "Quality validation failed: " + cause.getMessage());
                return Result.failure(
                    ErrorDetail.of(
                        "DATA_QUALITY_VALIDATION_FAILED",
                        com.bcbs239.regtech.core.domain.shared.ErrorType.SYSTEM_ERROR,
                        "Quality validation failed: " + cause.getMessage(),
                        "dataquality.validation.failed"
                    )
                );
            }
        }

        // Persist rule violations/execution logs once after validation completes.
        // This avoids per-worker database writes/transactions.
        rulesService.batchPersistValidationResults(command.batchId().value(), allValidationResults);

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

        logger.info("Quality validation completed: batchId={}, validExposures={}/{}, totalErrors={}, overallScore={}",
            command.batchId().value(),
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
                logger.debug("Detailed results stored in S3: {}", reference.uri());
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
        return Result.success();
    }

    private ValidationResultsDto validateSingleExposureNoPersist(ExposureRecord exposure) {
        return rulesService.validateConfigurableRulesNoPersist(exposure);
    }

    private ExposureValidationResult createExposureValidationResult(ValidationResultsDto validationResults) {
        List<ValidationError> errors = validationResults.validationErrors();

        // Group errors by dimension
        Map<QualityDimension, List<ValidationError>> dimensionErrors = new HashMap<>();
        for (QualityDimension dimension : QualityDimension.values()) {
            dimensionErrors.put(dimension, new ArrayList<>());
        }
        for (ValidationError error : errors) {
            dimensionErrors.get(error.dimension()).add(error);
        }

        return ExposureValidationResult.builder()
            .exposureId(validationResults.exposureId())
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

