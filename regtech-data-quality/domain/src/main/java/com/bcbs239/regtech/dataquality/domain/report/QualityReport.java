package com.bcbs239.regtech.dataquality.domain.report;


import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.events.*;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

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
    // Processing metadata (persisted/auditable)
    private Instant processingStartTime;
    private Instant processingEndTime;
    private Long processingDurationMs;

    // Private constructor - use factory methods
    private QualityReport() {}


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
    public Result<Void> startValidation() {
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
            reportId, batchId, bankId, updatedAt
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
    public Result<ValidationResult> recordValidationAndCalculateScores(ValidationResult validation) {
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
            reportId, batchId, bankId, validationSummary, updatedAt
        ));
        
        // Business Logic: Calculate quality scores using value object factory method
        // QualityScores knows how to create itself from validation results
        QualityScores qualityScores = QualityScores.calculateFrom(validation);
        
        // Business Logic: Store quality scores
        this.scores = qualityScores;
        this.updatedAt = Instant.now();
        
        // Emit domain event for score calculation
        addDomainEvent(new QualityScoresCalculatedEvent(
            reportId, batchId, bankId, qualityScores, updatedAt
        ));
        
        // Return validation result for further processing (e.g., S3 storage by application layer)
        return Result.success(validation);
    }
    
    /**
     * Records validation results from the quality validation engine.
     * Updates validation summary and raises domain event.
     */
    public Result<Void> recordValidationResults(ValidationResult validationResult) {
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
            reportId, batchId, bankId, validationSummary, updatedAt
        ));
        
        return Result.success();
    }
    
    /**
     * Calculates and stores quality scores based on validation results.
     * Updates quality scores and raises domain event.
     */
    public Result<Void> calculateScores(QualityScores qualityScores) {
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
        this.updatedAt = Instant.now();
        
        addDomainEvent(new QualityScoresCalculatedEvent(
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
    public Result<Void> completeValidation() {
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
        
        this.status = QualityStatus.COMPLETED;
        this.processingEndTime = Instant.now();
        
        // Calculate processing duration for audit/monitoring
        if (processingStartTime != null) {
            this.processingDurationMs = processingEndTime.toEpochMilli() - processingStartTime.toEpochMilli();
        }
        
        this.updatedAt = Instant.now();
        
        addDomainEvent(new QualityValidationCompletedEvent(
            reportId, batchId, bankId, scores, detailsReference, updatedAt
        ));
        
        return Result.success();
    }
    
    /**
     * Marks the quality validation as failed with an error message.
     * Transitions to FAILED status and raises failure event.
     */
    public Result<Void> markAsFailed(String errorMessage) {
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
            reportId, batchId, bankId, this.errorMessage, updatedAt
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

}

