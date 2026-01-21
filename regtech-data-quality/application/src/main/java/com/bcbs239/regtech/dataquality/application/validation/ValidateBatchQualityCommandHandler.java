package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.application.recommendations.RecommendationEngine;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.recommendations.QualityInsight;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.application.rulesengine.RuleViolationRepository;
import com.bcbs239.regtech.dataquality.application.validation.timeliness.TimelinessValidator;
import com.bcbs239.regtech.dataquality.domain.quality.DimensionScores;
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

import java.math.BigDecimal;
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
    private final RuleViolationRepository violationRepository;
    private final RecommendationEngine recommendationEngine;
    private final QualityDimensionMapper qualityDimensionMapper;
    private final TimelinessValidator timelinessValidator;

    public ValidateBatchQualityCommandHandler(
            IQualityReportRepository qualityReportRepository,
            S3StorageService s3StorageService,
            DataQualityRulesService rulesService,
            ParallelExposureValidationCoordinator coordinator,
            BaseUnitOfWork unitOfWork,
            RuleViolationRepository violationRepository,
            RecommendationEngine recommendationEngine,
            QualityDimensionMapper qualityDimensionMapper,
            TimelinessValidator timelinessValidator) {
        this.qualityReportRepository = qualityReportRepository;
        this.s3StorageService = s3StorageService;
        this.rulesService = rulesService;
        this.coordinator = coordinator;
        this.unitOfWork = unitOfWork;
        this.violationRepository = violationRepository;
        this.recommendationEngine = recommendationEngine;
        this.qualityDimensionMapper = qualityDimensionMapper;
        this.timelinessValidator = timelinessValidator;
    }

    @Timed(value = "dataquality.validation.batch", description = "Time taken to validate batch quality")
    public Result<Void> handle(ValidateBatchQualityCommand command) {
        logger.info("ValidateBatchQualityCommandHandler.handle start | batchId={} bankId={}",
                command.batchId().value(), command.bankId().value());

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

        // Download batch with metadata (declaredCount, reportDate)
        Result<BatchWithMetadata> downloadResult = s3StorageService.downloadBatchWithMetadata(command.s3Uri());

        if (downloadResult.isFailure()) {
            markReportAsFailed(report, "Failed to download batch data", CorrelationContext.correlationId());
            return Result.failure(downloadResult.errors());
        }
        BatchWithMetadata batchData = downloadResult.getValueOrThrow();
        List<ExposureRecord> exposures = batchData.exposures();

        logger.info("Downloaded batch for batchId={}: exposures={}, declaredCount={}, reportDate={}",
                command.batchId().value(), exposures.size(), batchData.declaredCount(), batchData.reportDate());

        ValidationBatchResult batchResult = coordinator.validateAll(
                exposures,
                rulesService,
                batchData.declaredCount() != null ? batchData.declaredCount() : command.expectedExposureCount(),
                null,  // crmReferences - requires bank CRM system integration
                command.uploadDate() != null ? command.uploadDate() : java.time.LocalDate.now(),
                command.bankId().value()
        );

        // Override timeliness calculation with metadata reportDate if available
        if (batchData.reportDate() != null && command.uploadDate() != null) {
            TimelinessValidator.TimelinessResult metadataTimeliness =
                    timelinessValidator.calculateTimeliness(
                            exposures,
                            command.uploadDate(),
                            batchData.reportDate(),  // Use metadata report date
                            command.bankId().value()
                    );
            batchResult = ValidationBatchResult.complete(
                    batchResult.results(),
                    batchResult.exposureResults(),
                    batchResult.consistencyResult(),
                    metadataTimeliness,  // Override with metadata-based calculation
                    batchResult.uniquenessResult()  // Keep existing uniqueness result
            );
        }

        List<ValidationResults> allResults = batchResult.results();
        Map<String, ExposureValidationResult> exposureResults = batchResult.exposureResults();

        violationRepository.insertViolations(command.batchId().value(), allResults.parallelStream().flatMap(r ->
                r.ruleViolations().stream()
        ).toList());

        // Batch-level validation (if needed in the future)
        List<ValidationError> batchErrors = new ArrayList<>();

        // Calculate dimension scores from per-exposure validation
        DimensionScores dimensionScores = ValidationResult.calculateDimensionScores(exposureResults, batchErrors, exposures.size());

        // Override timeliness score with calculated value from TimelinessValidator (if available)
        if (batchResult.hasTimelinessResult()) {
            var timelinessResult = batchResult.timelinessResult();
            dimensionScores = new DimensionScores(
                    dimensionScores.completeness(),
                    dimensionScores.accuracy(),
                    dimensionScores.consistency(),
                    timelinessResult.score(), // Override with calculated timeliness score
                    dimensionScores.uniqueness(),
                    dimensionScores.validity()
            );
        }

        // Map TimelinessResult to TimelinessDetails (if available)
        ValidationResult.TimelinessDetails timelinessDetails = null;
        if (batchResult.hasTimelinessResult()) {
            var tr = batchResult.timelinessResult();
            timelinessDetails = new ValidationResult.TimelinessDetails(
                    tr.reportingDate(),
                    tr.uploadDate(),
                    tr.delayDays(),
                    tr.thresholdDays(),
                    tr.score(),
                    tr.passed()
            );
        }

        // Create ValidationResult from the validated exposures
        ValidationResult validation = ValidationResult.builder()
                .exposureResults(exposureResults)
                .batchErrors(batchErrors)
                .allErrors(exposureResults.values().stream()
                        .flatMap(r -> r.errors().stream())
                        .toList())
                .dimensionScores(dimensionScores)  // Use overridden dimension scores
                .totalExposures(exposures.size())
                .validExposures((int) exposureResults.values().stream().filter(ExposureValidationResult::isValid).count())
                .consistencyDetails(batchResult.hasConsistencyChecks() ? batchResult.consistencyResult() : null)
                .timelinessDetails(timelinessDetails)
                .build();

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

        // Generate recommendations using RecommendationEngine
        // First, convert DimensionScores to a Map for the mapper
        java.util.Map<com.bcbs239.regtech.dataquality.domain.quality.QualityDimension, java.math.BigDecimal> dimensionScoresMap = new java.util.HashMap<>();
        DimensionScores aggregateDimensionScores = scores.getDimensionScores();
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.COMPLETENESS,
                java.math.BigDecimal.valueOf(aggregateDimensionScores.completeness()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.ACCURACY,
                java.math.BigDecimal.valueOf(aggregateDimensionScores.accuracy()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.CONSISTENCY,
                java.math.BigDecimal.valueOf(aggregateDimensionScores.consistency()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.TIMELINESS,
                java.math.BigDecimal.valueOf(aggregateDimensionScores.timeliness()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.UNIQUENESS,
                java.math.BigDecimal.valueOf(aggregateDimensionScores.uniqueness()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.VALIDITY,
                java.math.BigDecimal.valueOf(aggregateDimensionScores.validity()));

        java.util.Map<com.bcbs239.regtech.core.domain.quality.QualityDimension, java.math.BigDecimal> dimensionScoresCore =
                qualityDimensionMapper.toCoreQualityDimensions(dimensionScoresMap);

        String languageCode = "it"; // Italian for BCBS 239

        List<QualityInsight> recommendations = recommendationEngine.generateInsights(
                BigDecimal.valueOf(scores.overallScore()),
                dimensionScoresCore,
                languageCode
        );

        logger.info("Generated {} quality recommendations for batchId={} with overall score={}",
                recommendations.size(), command.batchId().value(), scores.overallScore());

        java.util.Map<String, String> metadata = java.util.Map.of(
                "batch-id", command.batchId().value(),
                "overall-score", String.valueOf(scores.overallScore()),
                "grade", scores.grade().name(),
                "compliant", String.valueOf(scores.isCompliant()),
                "total-exposures", String.valueOf(validation.totalExposures()),
                "valid-exposures", String.valueOf(validation.validExposures()),
                "total-errors", String.valueOf(validation.allErrors().size())
        );

        Result<S3Reference> s3Result = s3StorageService.storeDetailedResults(
                        command.batchId(), validation, metadata, recommendations)
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
        Result<Void> completeResult = report.completeValidation(command.filename());
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

        // Final completion log
        logger.info("ValidateBatchQualityCommandHandler.handle completed | batchId={} reportId={} overallScore={}",
                command.batchId().value(), report.getReportId().value(), scores.overallScore());

        return Result.success();
    }

    private void markReportAsFailed(QualityReport report, String errorMessage, String correlationId) {
        report.markAsFailed(errorMessage, correlationId);

        qualityReportRepository.save(report);

        unitOfWork.registerEntity(report);
        unitOfWork.saveChanges();
    }
}

