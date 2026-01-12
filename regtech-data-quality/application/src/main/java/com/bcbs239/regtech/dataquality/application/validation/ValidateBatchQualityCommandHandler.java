package com.bcbs239.regtech.dataquality.application.validation;

import com.bcbs239.regtech.core.application.BaseUnitOfWork;
import com.bcbs239.regtech.core.application.recommendations.RecommendationEngine;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.recommendations.QualityInsight;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.rulesengine.DataQualityRulesService;
import com.bcbs239.regtech.dataquality.application.rulesengine.RuleViolationRepository;
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

    public ValidateBatchQualityCommandHandler(
            IQualityReportRepository qualityReportRepository,
            S3StorageService s3StorageService,
            DataQualityRulesService rulesService,
            ParallelExposureValidationCoordinator coordinator,
            BaseUnitOfWork unitOfWork,
            RuleViolationRepository violationRepository,
            RecommendationEngine recommendationEngine,
            QualityDimensionMapper qualityDimensionMapper) {
        this.qualityReportRepository = qualityReportRepository;
        this.s3StorageService = s3StorageService;
        this.rulesService = rulesService;
        this.coordinator = coordinator;
        this.unitOfWork = unitOfWork;
        this.violationRepository = violationRepository;
        this.recommendationEngine = recommendationEngine;
        this.qualityDimensionMapper = qualityDimensionMapper;
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

        Result<List<ExposureRecord>> downloadResult = s3StorageService.downloadExposures(command.s3Uri(), command.expectedExposureCount());

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

        // Execute validation with consistency checks
        // declaredCount and crmReferences are optional - pass null for now
        ValidationBatchResult batchResult = coordinator.validateAll(
            exposures, 
            rulesService,
            null,  // declaredCount - TODO: get from batch metadata if available
            null   // crmReferences - TODO: get from CRM system if needed
        );

        List<ValidationResults> allResults = batchResult.results();
        Map<String, ExposureValidationResult> exposureResults = batchResult.exposureResults();

        violationRepository.insertViolations(command.batchId().value(), allResults.parallelStream().flatMap(r ->
                r.ruleViolations().stream()
        ).toList());

        // Batch-level validation (if needed in the future)
        List<ValidationError> batchErrors = new ArrayList<>();

        // Create ValidationResult from the validated exposures
        ValidationResult validation = ValidationResult.builder()
            .exposureResults(exposureResults)
            .batchErrors(batchErrors)
            .allErrors(exposureResults.values().stream()
                .flatMap(r -> r.errors().stream())
                .toList())
            .dimensionScores(ValidationResult.calculateDimensionScores(exposureResults, batchErrors, exposures.size()))
            .totalExposures(exposures.size())
            .validExposures((int) exposureResults.values().stream().filter(ExposureValidationResult::isValid).count())
            .consistencyDetails(batchResult.hasConsistencyChecks() ? batchResult.consistencyResult() : null)
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
        DimensionScores dimensionScores = scores.getDimensionScores();
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.COMPLETENESS, 
            java.math.BigDecimal.valueOf(dimensionScores.completeness()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.ACCURACY, 
            java.math.BigDecimal.valueOf(dimensionScores.accuracy()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.CONSISTENCY, 
            java.math.BigDecimal.valueOf(dimensionScores.consistency()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.TIMELINESS, 
            java.math.BigDecimal.valueOf(dimensionScores.timeliness()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.UNIQUENESS, 
            java.math.BigDecimal.valueOf(dimensionScores.uniqueness()));
        dimensionScoresMap.put(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.VALIDITY, 
            java.math.BigDecimal.valueOf(dimensionScores.validity()));
        
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

