package com.bcbs239.regtech.dataquality.domain.report;

import com.bcbs239.regtech.core.shared.Entity;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.events.*;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import lombok.Getter;

import java.time.Instant;

/**
 * Quality Report aggregate root that manages the lifecycle of data quality validation
 * for a batch of exposures. Implements business rules for state transitions and
 * publishes domain events for quality processing lifecycle.
 */
@Getter
public class QualityReport extends Entity {

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

    // Explicit setters used by mappers/repository when hydrating aggregate from persistence
    public void setReportId(QualityReportId reportId) { this.reportId = reportId; }
    public void setBatchId(BatchId batchId) { this.batchId = batchId; }
    public void setBankId(BankId bankId) { this.bankId = bankId; }
    public void setStatus(QualityStatus status) { this.status = status; }
    public void setScores(QualityScores scores) { this.scores = scores; }
    public void setValidationSummary(ValidationSummary validationSummary) { this.validationSummary = validationSummary; }
    public void setDetailsReference(S3Reference detailsReference) { this.detailsReference = detailsReference; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setProcessingStartTime(Instant processingStartTime) { this.processingStartTime = processingStartTime; }
    public void setProcessingEndTime(Instant processingEndTime) { this.processingEndTime = processingEndTime; }
    public void setProcessingDurationMs(Long processingDurationMs) { this.processingDurationMs = processingDurationMs; }

    // Getters for new fields (lombok @Getter is not reliable due to manual edits; provide explicit getters)
    public Instant getProcessingStartTime() { return processingStartTime; }
    public Instant getProcessingEndTime() { return processingEndTime; }
    public Long getProcessingDurationMs() { return processingDurationMs; }

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
        report.scores = null; // Will be set when calculated
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
                "Cannot start validation from status: " + status,
                "status"
            ));
        }
        
        this.status = QualityStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
        
        addDomainEvent(new QualityValidationStartedEvent(
            reportId, batchId, bankId, updatedAt
        ));
        
        return Result.success();
    }
    
    /**
     * Records validation results from the quality validation engine.
     * Updates validation summary and raises domain event.
     */
    public Result<Void> recordValidationResults(ValidationResult validationResult) {
        if (!canRecordResults()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                "Cannot record results from status: " + status,
                "status"
            ));
        }
        
        if (validationResult == null) {
            return Result.failure(ErrorDetail.of(
                "VALIDATION_RESULT_NULL",
                "Validation result cannot be null",
                "validationResult"
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
                "Cannot calculate scores from status: " + status,
                "status"
            ));
        }
        
        if (qualityScores == null) {
            return Result.failure(ErrorDetail.of(
                "QUALITY_SCORES_NULL",
                "Quality scores cannot be null",
                "qualityScores"
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
                "Cannot store detailed results from status: " + status,
                "status"
            ));
        }
        
        if (s3Reference == null) {
            return Result.failure(ErrorDetail.of(
                "S3_REFERENCE_NULL",
                "S3 reference cannot be null",
                "s3Reference"
            ));
        }
        
        this.detailsReference = s3Reference;
        this.updatedAt = Instant.now();
        
        return Result.success();
    }
    
    /**
     * Completes the quality validation process successfully.
     * Transitions to COMPLETED status and raises completion event.
     */
    public Result<Void> completeValidation() {
        if (!isInProgress()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_STATE_TRANSITION",
                "Cannot complete validation from status: " + status,
                "status"
            ));
        }
        
        if (scores == null) {
            return Result.failure(ErrorDetail.of(
                "SCORES_NOT_CALCULATED",
                "Quality scores must be calculated before completion",
                "scores"
            ));
        }
        
        if (detailsReference == null) {
            return Result.failure(ErrorDetail.of(
                "DETAILS_NOT_STORED",
                "Detailed results must be stored before completion",
                "detailsReference"
            ));
        }
        
        this.status = QualityStatus.COMPLETED;
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
                "Cannot mark as failed from terminal status: " + status,
                "status"
            ));
        }
        
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "ERROR_MESSAGE_REQUIRED",
                "Error message is required when marking as failed",
                "errorMessage"
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

