package com.bcbs239.regtech.dataquality.domain.report;


import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.QualityGrade;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.events.*;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.ActionPresentation;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.ExposurePresentation;
import com.bcbs239.regtech.dataquality.domain.model.presentation.QualityReportPresentation.ViolationPresentation;
import com.bcbs239.regtech.dataquality.domain.model.valueobject.LargeExposure;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.Instant;

/**
 * Quality Report aggregate root that manages the lifecycle of data quality validation
 * for a batch of exposures. Implements business rules for state transitions and
 * publishes domain events for quality processing lifecycle.
 */
@Setter
@Getter
public class QualityReport extends Entity {

    // Explicit setters used by mappers/repository when hydrating aggregate from persistence
    // Getters
    private QualityReportId reportId;
    private BatchId batchId;
    private BankId bankId;
    private QualityStatus status;
    private QualityScores scores;
    private ValidationSummary validationSummary;
    private S3Reference detailsReference;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;
    private QualityGrade qualityGrade;
    
    // File Metadata
    private FileMetadata fileMetadata;

    // Processing metadata (persisted/auditable)
    private Instant processingStartTime;
    private Instant processingEndTime;
    private Long processingDurationMs;

    // Private constructor - use factory methods
    private QualityReport() {}

    /**
     * Generate frontend presentation model.
     *
     * <p>DOMAIN BEHAVIOR: the aggregate knows how to present itself for the frontend.
     * This is a read-only presentation model and does not mutate domain state.</p>
     */
    public QualityReportPresentation toFrontendPresentation(List<LargeExposure> largeExposures) {
        return toFrontendPresentation(largeExposures, null);
    }

    /**
     * Generate frontend presentation model using an optional summary override.
     *
     * <p>This allows the application/infrastructure layer to derive a richer
     * {@link ValidationSummary} from stored detailed results without performing I/O in the domain.
     * The aggregate is not mutated.</p>
     */
    public QualityReportPresentation toFrontendPresentation(List<LargeExposure> largeExposures, ValidationSummary summaryOverride) {
        ValidationSummary safeSummary = summaryOverride != null
            ? summaryOverride
            : (validationSummary != null ? validationSummary : ValidationSummary.empty());

        QualityScores safeScores = scores != null ? scores : QualityScores.empty();
        List<LargeExposure> safeLargeExposures = largeExposures != null ? largeExposures : List.of();

        return new QualityReportPresentation(
            extractFileName(),
            calculateFileSize(safeSummary),
            safeSummary.totalExposures(),
            roundScore(safeScores.overallScore()),
            roundScore(safeSummary.getValidationRatePercentage()),
            countCriticalViolations(safeSummary, safeScores),
            safeLargeExposures.size(),
            generateViolations(safeSummary, safeScores, safeLargeExposures),
            generateTopExposures(safeLargeExposures),
            generateActions(safeSummary, safeScores, safeLargeExposures)
        );
    }


    /**
     * Factory method to create a new quality report for a batch.
     * Initializes the report in PENDING status ready for validation.
     */
    public static QualityReport createForBatch(BatchId batchId, BankId bankId) {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (bankId == null) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
        
        QualityReport report = new QualityReport();
        report.reportId = QualityReportId.generate();
        report.batchId = batchId;
        report.bankId = bankId;
        report.status = QualityStatus.PENDING;
        report.createdAt = Instant.now();
        report.updatedAt = Instant.now();
        
        return report;
    }
    
    /**
     * Starts the quality validation process.
     * Transitions from PENDING to IN_PROGRESS status and raises domain event.
     */
    public Result<Void> startValidation(String correlationId) {
        if (!canStartValidation()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                ErrorType.VALIDATION_ERROR,
                "Cannot start validation from status: " + status,
                "quality.report.invalid.state.transition"
            ));
        }
        
        this.status = QualityStatus.IN_PROGRESS;
        this.processingStartTime = Instant.now();
        this.updatedAt = Instant.now();
        
        addDomainEvent(new QualityValidationStartedEvent(
            reportId, batchId, bankId, updatedAt, correlationId
        ));
        
        return Result.success();
    }
    
    /**
     * Records validation results and calculates quality scores.
     * This method accepts pre-validated results from the application layer.
     * 
     * <p>This follows proper DDD - the aggregate doesn't orchestrate infrastructure concerns,
     * it just records the results and maintains its invariants.</p>
     * 
     * @param validation The validation results from the Rules Engine
     * @return Result containing ValidationResult for further processing (e.g., S3 storage)
     */
    public Result<ValidationResult> recordValidationAndCalculateScores(ValidationResult validation, String correlationId) {
        // Guard: Ensure we're in the correct state
        if (!isInProgress()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                ErrorType.VALIDATION_ERROR,
                "Cannot record validation from status: " + status + ". Must be IN_PROGRESS.",
                "quality.report.invalid.state.transition"
            ));
        }
        
        // Guard: Validate inputs
        if (validation == null) {
            return Result.failure(ErrorDetail.of(
                "VALIDATION_RESULT_NULL",
                ErrorType.VALIDATION_ERROR,
                "Validation result cannot be null",
                "quality.report.validation.result.null"
            ));
        }
        
        // Business Logic: Store validation summary
        this.validationSummary = validation.summary();
        this.updatedAt = Instant.now();
        
        // Emit domain event for validation results
        addDomainEvent(new QualityResultsRecordedEvent(
            reportId, batchId, bankId, validationSummary, updatedAt,correlationId
        ));
        
        // Business Logic: Calculate quality scores using value object factory method
        // QualityScores knows how to create itself from validation results
        QualityScores qualityScores = QualityScores.calculateFrom(validation);
        
        // Business Logic: Store quality scores and grade
        this.scores = qualityScores;
        this.qualityGrade = qualityScores.grade();
        this.updatedAt = Instant.now();
        
        // Emit domain event for score calculation
        addDomainEvent(new QualityScoresCalculatedEvent(correlationId,
            reportId, batchId, bankId, qualityScores, updatedAt
        ));
        
        // Return validation result for further processing (e.g., S3 storage by application layer)
        return Result.success(validation);
    }
    
    /**
     * Records validation results from the quality validation engine.
     * Updates validation summary and raises domain event.
     */
    public Result<Void> recordValidationResults(ValidationResult validationResult, String correlationId) {
        if (!canRecordResults()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                ErrorType.VALIDATION_ERROR,
                "Cannot record results from status: " + status,
                "quality.report.invalid.state.transition"
            ));
        }
        
        if (validationResult == null) {
            return Result.failure(ErrorDetail.of(
                "VALIDATION_RESULT_NULL",
                ErrorType.VALIDATION_ERROR,
                "Validation result cannot be null",
                "quality.report.validation.result.null"
            ));
        }
        
        this.validationSummary = validationResult.summary();
        this.updatedAt = Instant.now();
        
        addDomainEvent(new QualityResultsRecordedEvent(
            reportId, batchId, bankId, validationSummary, updatedAt, correlationId
        ));
        
        return Result.success();
    }
    
    /**
     * Calculates and stores quality scores based on validation results.
     * Updates quality scores and raises domain event.
     */
    public Result<Void> calculateScores(QualityScores qualityScores, String correlationId) {
        if (!canCalculateScores()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                ErrorType.VALIDATION_ERROR,
                "Cannot calculate scores from status: " + status,
                "quality.report.invalid.state.transition"
            ));
        }
        
        if (qualityScores == null) {
            return Result.failure(ErrorDetail.of(
                "QUALITY_SCORES_NULL",
                ErrorType.VALIDATION_ERROR,
                "Quality scores cannot be null",
                "quality.report.quality.scores.null"
            ));
        }
        
        this.scores = qualityScores;
        this.qualityGrade = qualityScores.grade();
        this.updatedAt = Instant.now();
        
        addDomainEvent(new QualityScoresCalculatedEvent(correlationId,
            reportId, batchId, bankId, qualityScores, updatedAt
        ));
        
        return Result.success();
    }
    
    /**
     * Stores reference to detailed validation results in S3.
     * Updates S3 reference for detailed results access.
     */
    public Result<Void> storeDetailedResults(S3Reference s3Reference) {
        if (!isInProgress()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                ErrorType.VALIDATION_ERROR,
                "Cannot store detailed results from status: " + status,
                "quality.report.invalid.state.transition"
            ));
        }
        
        if (s3Reference == null) {
            return Result.failure(ErrorDetail.of(
                "S3_REFERENCE_NULL",
                ErrorType.VALIDATION_ERROR,
                "S3 reference cannot be null",
                "quality.report.s3.reference.null"
            ));
        }
        
        this.detailsReference = s3Reference;
        this.updatedAt = Instant.now();
        
        return Result.success();
    }
    
    /**
     * Completes the quality validation process successfully.
     * Transitions to COMPLETED status and raises completion event.
     * Calculates processing duration for auditing purposes.
     */
    public Result<Void> completeValidation(String filename) {
        if (!isInProgress()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                ErrorType.VALIDATION_ERROR,
                "Cannot complete validation from status: " + status,
                "quality.report.invalid.state.transition"
            ));
        }
        
        if (scores == null) {
            return Result.failure(ErrorDetail.of(
                "SCORES_NOT_CALCULATED",
                ErrorType.VALIDATION_ERROR,
                "Quality scores must be calculated before completion",
                "quality.report.scores.not.calculated"
            ));
        }
        
        if (detailsReference == null) {
            return Result.failure(ErrorDetail.of(
                "DETAILS_NOT_STORED",
                ErrorType.VALIDATION_ERROR,
                "Detailed results must be stored before completion",
                "quality.report.details.not.stored"
            ));
        }

        if (this.qualityGrade == null) {
            this.qualityGrade = this.scores.grade();
        }
        
        this.status = QualityStatus.COMPLETED;
        this.processingEndTime = Instant.now();
        
        // Calculate processing duration for audit/monitoring
        if (processingStartTime != null) {
            this.processingDurationMs = processingEndTime.toEpochMilli() - processingStartTime.toEpochMilli();
        }
        
        this.updatedAt = Instant.now();
        
        addDomainEvent(new QualityValidationCompletedEvent(
            reportId, batchId, bankId, scores, qualityGrade, detailsReference, updatedAt, filename
        ));
        
        return Result.success();
    }
    
    /**
     * Marks the quality validation as failed with an error message.
     * Transitions to FAILED status and raises failure event.
     */
    public Result<Void> markAsFailed(String errorMessage, String correlationId) {
        if (isTerminal()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                ErrorType.VALIDATION_ERROR,
                "Cannot mark as failed from terminal status: " + status,
                "quality.report.invalid.state.transition"
            ));
        }
        
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "ERROR_MESSAGE_REQUIRED",
                ErrorType.VALIDATION_ERROR,
                "Error message is required when marking as failed",
                "quality.report.error.message.required"
            ));
        }
        
        this.status = QualityStatus.FAILED;
        this.errorMessage = errorMessage.trim();
        this.updatedAt = Instant.now();
        
        addDomainEvent(new QualityValidationFailedEvent(
            reportId, batchId, bankId, this.errorMessage, updatedAt, correlationId
        ));
        
        return Result.success();
    }
    
    // State query methods
    
    /**
     * Checks if validation can be started (status is PENDING).
     */
    public boolean canStartValidation() {
        return status == QualityStatus.PENDING;
    }
    
    /**
     * Checks if validation results can be recorded (status is IN_PROGRESS).
     */
    public boolean canRecordResults() {
        return status == QualityStatus.IN_PROGRESS;
    }
    
    /**
     * Checks if quality scores can be calculated (status is IN_PROGRESS and results recorded).
     */
    public boolean canCalculateScores() {
        return status == QualityStatus.IN_PROGRESS && validationSummary != null;
    }
    
    /**
     * Checks if validation can be completed (status is IN_PROGRESS, scores calculated, details stored).
     */
    public boolean canCompleteValidation() {
        return status == QualityStatus.IN_PROGRESS && 
               scores != null && 
               detailsReference != null;
    }
    
    /**
     * Checks if the report is currently in progress.
     */
    public boolean isInProgress() {
        return status == QualityStatus.IN_PROGRESS;
    }
    
    /**
     * Checks if the report is in a terminal state (completed or failed).
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }
    
    /**
     * Checks if the validation completed successfully.
     */
    public boolean isCompleted() {
        return status == QualityStatus.COMPLETED;
    }
    
    /**
     * Checks if the validation failed.
     */
    public boolean isFailed() {
        return status == QualityStatus.FAILED;
    }
    
    /**
     * Checks if the quality meets compliance standards.
     */
    public boolean isCompliant() {
        return scores != null && scores.isCompliant();
    }
    
    /**
     * Checks if the quality requires immediate attention.
     */
    public boolean requiresAttention() {
        return scores != null && scores.requiresAttention();
    }

    // =====================================================================
    // Presentation capabilities (read-only)
    // =====================================================================

    private String extractFileName() {
        if (this.fileMetadata != null) {
            return this.fileMetadata.filename();
        }
        if (batchId == null || batchId.value() == null) {
            return "esposizioni.xlsx";
        }

        String batchIdStr = batchId.value();
        try {
            String[] parts = batchIdStr.split("_");
            if (parts.length >= 2) {
                String dateStr = parts[1]; // YYYYMMDD
                if (dateStr.length() >= 6) {
                    int year = Integer.parseInt(dateStr.substring(0, 4));
                    int month = Integer.parseInt(dateStr.substring(4, 6));

                    return String.format("esposizioni_%s_%d.xlsx", getItalianMonth(month), year);
                }
            }
        } catch (IndexOutOfBoundsException | IllegalArgumentException ignored) {
            // Intentionally ignore and fall back to default name
        }

        return "esposizioni.xlsx";
    }

    private String calculateFileSize(ValidationSummary summary) {
        if (this.fileMetadata != null) {
             long bytes = this.fileMetadata.size();
             if (bytes < 1024L * 1024L) {
                 return String.format(Locale.ITALY, "%.1f KB", bytes / 1024.0);
             }
             return String.format(Locale.ITALY, "%.1f MB", bytes / (1024.0 * 1024.0));
        }
        
        int total = summary != null ? summary.totalExposures() : 0;
        long bytes = total * 150L;

        if (bytes < 1024L * 1024L) {
            return String.format(Locale.ITALY, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ITALY, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private int countCriticalViolations(ValidationSummary summary, QualityScores safeScores) {
        if (summary == null) {
            return 0;
        }

        Map<ValidationError.ErrorSeverity, Integer> bySeverity = summary.errorsBySeverity();
        if (bySeverity == null || bySeverity.isEmpty()) {
            if (safeScores == null) {
                return 0;
            }
            return (int) Arrays.stream(QualityDimension.values())
                .filter(dim -> safeScores.getScore(dim) < 50.0)
                .count();
        }

        return bySeverity.getOrDefault(ValidationError.ErrorSeverity.CRITICAL, 0);
    }

    private List<ViolationPresentation> generateViolations(
        ValidationSummary summary,
        QualityScores safeScores,
        List<LargeExposure> largeExposures
    ) {
        ValidationSummary safeSummary = summary != null ? summary : ValidationSummary.empty();
        QualityScores scoresSafe = safeScores != null ? safeScores : QualityScores.empty();
        List<LargeExposure> safeLarge = largeExposures != null ? largeExposures : List.of();

        List<ViolationPresentation> violations = new ArrayList<>();

        // 0) Specific error-code driven violations (to match frontend expectations)
        violations.addAll(generateViolationFromErrorCodes(safeSummary));

        // 1) Large exposure violations
        safeLarge.stream()
            .filter(LargeExposure::exceedsLimit)
            .forEach(exposure -> violations.add(
                new ViolationPresentation(
                    "Superamento Limite Grande Esposizione",
                    "CRITICA",
                    String.format(Locale.ITALY, "%s supera il limite del 25%% con %.2f%%",
                        exposure.counterparty(), exposure.percentOfCapital()),
                    Map.of(
                        "Controparte", exposure.counterparty(),
                        "Esposizione", formatCurrency(exposure.amount()),
                        "Percentuale", formatPercent(exposure.percentOfCapital()) + "%",
                        "Limite", "25.00%"
                    )
                )
            ));

        // 2) Dimension violations
        Map<QualityDimension, Integer> errorsByDim = safeSummary.errorsByDimension();
        if (errorsByDim != null) {
            errorsByDim.forEach((dimension, errorCount) -> {
                if (dimension != null && errorCount != null && errorCount > 0) {
                    violations.add(createDimensionViolation(safeSummary, scoresSafe, dimension, errorCount));
                }
            });
        }

        violations.sort(Comparator.comparing(v -> getSeverityOrder(v.severity())));
        return violations;
    }

    private List<ViolationPresentation> generateViolationFromErrorCodes(ValidationSummary summary) {
        if (summary == null || summary.errorsByCode() == null || summary.errorsByCode().isEmpty()) {
            return List.of();
        }

        int total = Math.max(1, summary.totalExposures());
        Map<String, Integer> codes = summary.errorsByCode();

        int missingMaturityDate = sumMatchingCodes(codes, "DATA_SCADENZA", "_MISSING", "_REQUIRED");
        int invalidDateFormat = sumMatchingCodes(codes, "DATA", "_INVALID_FORMAT");

        List<ViolationPresentation> violations = new ArrayList<>();

        if (missingMaturityDate > 0) {
            violations.add(new ViolationPresentation(
                "Dati Mancanti - Campo Obbligatorio",
                "CRITICA",
                String.format(Locale.ITALY, "%s record senza data di scadenza",
                    formatNumber(missingMaturityDate)
                ),
                Map.of(
                    "Campo", "Data Scadenza",
                    "Record Interessati", formatNumber(missingMaturityDate),
                    "Percentuale", formatPercent((missingMaturityDate * 100.0) / total) + "%"
                )
            ));
        }

        if (invalidDateFormat > 0) {
            violations.add(new ViolationPresentation(
                "Formato Data Non Standard",
                "ALTA",
                String.format(Locale.ITALY, "%s record con formato data non valido",
                    formatNumber(invalidDateFormat)
                ),
                Map.of(
                    "Tipo", "Data",
                    "Record Interessati", formatNumber(invalidDateFormat),
                    "Percentuale", formatPercent((invalidDateFormat * 100.0) / total) + "%"
                )
            ));
        }

        return violations;
    }

    private int sumMatchingCodes(Map<String, Integer> codes, String requiredSubstring, String... requiredPatterns) {
        if (codes == null || codes.isEmpty()) {
            return 0;
        }

        return codes.entrySet().stream()
            .filter(e -> e.getKey() != null)
            .filter(e -> requiredSubstring == null || e.getKey().contains(requiredSubstring))
            .filter(e -> {
                if (requiredPatterns == null || requiredPatterns.length == 0) {
                    return true;
                }
                for (String p : requiredPatterns) {
                    if (p != null && e.getKey().contains(p)) {
                        return true;
                    }
                }
                return false;
            })
            .mapToInt(e -> e.getValue() != null ? e.getValue() : 0)
            .sum();
    }

    private ViolationPresentation createDimensionViolation(
        ValidationSummary summary,
        QualityScores scoresSafe,
        QualityDimension dimension,
        int errorCount
    ) {
        int total = Math.max(1, summary.totalExposures());
        double score = scoresSafe.getScore(dimension);
        int exposuresWithErrors = countExposuresWithDimensionErrors(summary, dimension);
        double errorRate = (exposuresWithErrors * 100.0) / total;
        errorRate = Math.min(100.0, Math.max(0.0, errorRate));

        String dimensionName = getItalianDimensionName(dimension);

        return new ViolationPresentation(
            String.format("%s - %s Errori", dimensionName, formatNumber(errorCount)),
            calculateSeverity(score, errorRate),
            String.format(Locale.ITALY, "%s record con errori di %s (%.2f%% del totale)",
                formatNumber(exposuresWithErrors),
                dimensionName.toLowerCase(Locale.ITALY),
                errorRate
            ),
            Map.of(
                "Dimensione", dimensionName,
                "Errori", formatNumber(errorCount),
                "Record Interessati", formatNumber(exposuresWithErrors),
                "Score", String.format(Locale.ITALY, "%.1f%%", score)
            )
        );
    }

    /**
     * Counts exposures affected for a given dimension.
     *
     * <p>We intentionally count exposures (records) rather than total errors so the resulting
     * error rate cannot exceed 100%.
     *
     * <p>If exposure-level results are not available in the aggregate, we estimate affected
     * exposures by distributing invalid exposures proportionally across dimensions.
     */
    private int countExposuresWithDimensionErrors(ValidationSummary summary, QualityDimension dimension) {
        if (summary == null || dimension == null) {
            return 0;
        }

        int total = summary.totalExposures();
        if (total <= 0) {
            return 0;
        }

        int invalid = summary.invalidExposures();
        if (invalid <= 0) {
            invalid = Math.max(0, total - Math.max(0, summary.validExposures()));
        }
        if (invalid <= 0) {
            return 0;
        }

        Map<QualityDimension, Integer> byDim = summary.errorsByDimension();
        if (byDim == null || byDim.isEmpty()) {
            return Math.min(invalid, total);
        }

        int errorsInThisDim = byDim.getOrDefault(dimension, 0);
        if (errorsInThisDim <= 0) {
            return 0;
        }

        int totalErrorsAllDims = byDim.values().stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        if (totalErrorsAllDims <= 0) {
            return Math.min(invalid, total);
        }

        double proportion = errorsInThisDim / (double) totalErrorsAllDims;
        int estimated = (int) Math.ceil(proportion * invalid);
        int clamped = Math.max(1, Math.min(estimated, Math.min(invalid, total)));
        return clamped;
    }

    private List<ExposurePresentation> generateTopExposures(List<LargeExposure> largeExposures) {
        List<LargeExposure> safeLarge = largeExposures != null ? largeExposures : List.of();

        return safeLarge.stream()
            .sorted(Comparator.comparing(LargeExposure::amount).reversed())
            .limit(5)
            .map(e -> new ExposurePresentation(
                e.counterparty(),
                formatCurrency(e.amount()),
                formatPercent(e.percentOfCapital()) + "%",
                e.exceedsLimit() ? "Violazione" : "Conforme"
            ))
            .collect(Collectors.toList());
    }

    private List<ActionPresentation> generateActions(
        ValidationSummary summary,
        QualityScores safeScores,
        List<LargeExposure> largeExposures
    ) {
        ValidationSummary safeSummary = summary != null ? summary : ValidationSummary.empty();
        QualityScores scoresSafe = safeScores != null ? safeScores : QualityScores.empty();
        List<LargeExposure> safeLarge = largeExposures != null ? largeExposures : List.of();

        List<ActionPresentation> actions = new ArrayList<>();

        // Action for large exposure violations
        safeLarge.stream()
            .filter(LargeExposure::exceedsLimit)
            .findFirst()
            .ifPresent(worst -> actions.add(new ActionPresentation(
                "Ridurre Esposizione " + worst.counterparty(),
                "Vendere " + formatCurrency(worst.calculateExcess()),
                "30 giorni",
                "Critica",
                "red"
            )));

        // Action for worst dimension
        QualityDimension worstDimension = scoresSafe.getLowestScoringDimension();
        int totalErrorsInDimension = safeSummary.getErrorCountForDimension(worstDimension);
        int exposuresWithErrors = countExposuresWithDimensionErrors(safeSummary, worstDimension);

        if (exposuresWithErrors > 0) {
            int total = Math.max(1, safeSummary.totalExposures());
            double errorRate = (exposuresWithErrors * 100.0) / total;
            errorRate = Math.min(100.0, Math.max(0.0, errorRate));

            actions.add(new ActionPresentation(
                "Correggere " + getItalianDimensionName(worstDimension),
                String.format(Locale.ITALY, "%s record con %s errori da risolvere",
                    formatNumber(exposuresWithErrors),
                    formatNumber(totalErrorsInDimension)
                ),
                calculateDeadline(errorRate),
                calculatePriority(errorRate),
                getPriorityColor(errorRate)
            ));
        }

        return actions;
    }

    // =====================================================================
    // Domain rules used for presentation
    // =====================================================================

    private String calculateSeverity(double score, double errorRate) {
        if (score < 50 || errorRate > 10) return "CRITICA";
        if (score < 75 || errorRate > 5) return "ALTA";
        if (score < 90 || errorRate > 1) return "MEDIA";
        return "BASSA";
    }

    private String calculateDeadline(double errorRate) {
        if (errorRate > 10) return "7 giorni";
        if (errorRate > 5) return "14 giorni";
        return "30 giorni";
    }

    private String calculatePriority(double errorRate) {
        if (errorRate > 10) return "Critica";
        if (errorRate > 5) return "Alta";
        return "Media";
    }

    private String getPriorityColor(double errorRate) {
        if (errorRate > 10) return "red";
        if (errorRate > 5) return "orange";
        return "yellow";
    }

    // =====================================================================
    // Formatting utilities
    // =====================================================================

    private String formatCurrency(BigDecimal amount) {
        BigDecimal safe = amount != null ? amount : BigDecimal.ZERO;
        return "€" + NumberFormat.getInstance(Locale.ITALY).format(safe.longValue());
    }

    private String formatPercent(double percent) {
        return String.format(Locale.ITALY, "%.2f", percent);
    }

    private String formatNumber(int number) {
        return NumberFormat.getInstance(Locale.ITALY).format(number);
    }

    private double roundScore(double score) {
        return Math.round(score * 10.0) / 10.0;
    }

    private String getItalianMonth(int month) {
        String[] months = {
            "gennaio", "febbraio", "marzo", "aprile",
            "maggio", "giugno", "luglio", "agosto",
            "settembre", "ottobre", "novembre", "dicembre"
        };
        if (month < 1 || month > 12) {
            return "mese";
        }
        return months[month - 1];
    }

    private int getSeverityOrder(String severity) {
        String safe = Objects.requireNonNullElse(severity, "");
        return switch (safe) {
            case "CRITICA" -> 0;
            case "ALTA" -> 1;
            case "MEDIA" -> 2;
            default -> 3;
        };
    }

    private String getItalianDimensionName(QualityDimension dimension) {
        if (dimension == null) {
            return "Qualità";
        }
        String italianName = dimension.getItalianName();
        return (italianName == null || italianName.isBlank()) ? dimension.getDisplayName() : italianName;
    }

}

