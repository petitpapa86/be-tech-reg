package com.bcbs239.regtech.ingestion.domain.batch;


import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.shared.*;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankInfo;
import com.bcbs239.regtech.ingestion.domain.batch.rules.BatchSpecifications;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing an ingestion batch with its complete lifecycle.
 */
@Getter
public class IngestionBatch extends Entity {

    private final BatchId batchId;
    private final BankId bankId;
    // Manual getters for Lombok compatibility issues
    private BatchStatus status;
    private final FileMetadata fileMetadata;
    private S3Reference s3Reference;
    private BankInfo bankInfo;
    private Integer totalExposures;
    private final Instant uploadedAt;
    private Instant completedAt;
    private String errorMessage;
    private Long processingDurationMs;
    private int recoveryAttempts = 0;
    private Instant lastCheckpoint;
    private String checkpointData;
    private Instant updatedAt;
    
    /**
     * Constructor for creating a new ingestion batch.
     */
    public IngestionBatch(BatchId batchId, BankId bankId, FileMetadata fileMetadata) {
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.bankId = Objects.requireNonNull(bankId, "Bank ID cannot be null");
        this.fileMetadata = Objects.requireNonNull(fileMetadata, "File metadata cannot be null");
        this.status = BatchStatus.UPLOADED;
        this.uploadedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Constructor for reconstituting from persistence.
     */
    public IngestionBatch(BatchId batchId, BankId bankId, BatchStatus status, 
                         FileMetadata fileMetadata, S3Reference s3Reference, 
                         BankInfo bankInfo, Integer totalExposures, 
                         Instant uploadedAt, Instant completedAt, 
                         String errorMessage, Long processingDurationMs,
                         int recoveryAttempts, Instant lastCheckpoint, 
                         String checkpointData, Instant updatedAt) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.status = status;
        this.fileMetadata = fileMetadata;
        this.s3Reference = s3Reference;
        this.bankInfo = bankInfo;
        this.totalExposures = totalExposures;
        this.uploadedAt = uploadedAt;
        this.completedAt = completedAt;
        this.errorMessage = errorMessage;
        this.processingDurationMs = processingDurationMs;
        this.recoveryAttempts = recoveryAttempts;
        this.lastCheckpoint = lastCheckpoint;
        this.checkpointData = checkpointData;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }
    
    /**
     * Start processing the batch by transitioning to PARSING status.
     */
    public Result<Void> startProcessing() {
        // Business rules: cannot start if batch is terminal
        Result<Void> spec = BatchSpecifications.mustNotBeTerminal().isSatisfiedBy(this);
        if (spec.isFailure()) return spec;

        // Validate transition
        Result<Void> vt = BatchTransitions.validateTransition(this, BatchStatus.PARSING);
        if (vt.isFailure()) return vt;

        // Apply transition
        BatchTransitions.applyTransition(this, BatchStatus.PARSING);
        addDomainEvent(new BatchProcessingStartedEvent(batchId, bankId, Instant.now(), batchId.value()));

        return Result.success(null);
    }
    
    /**
     * Mark the batch as validated with the exposure count.
     */
    public Result<Void> markAsValidated(int exposureCount) {
        // Validate exposure count
        if (exposureCount < 0) {
            return Result.failure(ErrorDetail.of("INVALID_EXPOSURE_COUNT", ErrorType.VALIDATION_ERROR, "Exposure count cannot be negative", "ingestion.batch.invalidExposureCount"));
        }

        // Transition validation
        Result<Void> vt = BatchTransitions.validateTransition(this, BatchStatus.VALIDATED);
        if (vt.isFailure()) return vt;

        // Apply
        BatchTransitions.applyTransition(this, BatchStatus.VALIDATED);
        this.totalExposures = exposureCount;
        addDomainEvent(new BatchValidatedEvent(batchId, bankId, exposureCount, Instant.now(), batchId.value()));

        return Result.success(null);
    }
    
    /**
     * Attach bank information to the batch.
     */
    public Result<Void> attachBankInfo(BankInfo bankInfo) {
        Objects.requireNonNull(bankInfo, "Bank info cannot be null");
        
        if (!bankInfo.bankId().equals(this.bankId)) {
            return Result.failure(ErrorDetail.of("BANK_ID_MISMATCH", ErrorType.VALIDATION_ERROR, "Bank info does not match batch bank ID", "ingestion.batch.bankIdMismatch"));
        }
        
        if (!bankInfo.isActive()) {
            return Result.failure(ErrorDetail.of("BANK_INACTIVE", ErrorType.BUSINESS_RULE_ERROR, "Cannot process batch for inactive bank", "ingestion.batch.bankInactive"));
        }
        
        this.bankInfo = bankInfo;
        return Result.success(null);
    }
    
    /**
     * Record S3 storage reference and transition to STORING status.
     */
    public Result<Void> recordS3Storage(S3Reference s3Reference) {
        Objects.requireNonNull(s3Reference, "S3 reference cannot be null");
        
        // Apply business rule: batch must not be terminal and must be validated
        Result<Void> spec = BatchSpecifications.mustNotBeTerminal().and(BatchSpecifications.mustHaveExposureCount()).isSatisfiedBy(this);
        if (spec.isFailure()) return spec;

        // Transition validation
        Result<Void> vt = BatchTransitions.validateTransition(this, BatchStatus.STORING);
        if (vt.isFailure()) return vt;

        BatchTransitions.applyTransition(this, BatchStatus.STORING);
        this.s3Reference = s3Reference;
        addDomainEvent(new BatchStoredEvent(batchId, bankId, s3Reference, Instant.now(), batchId.value()));

        return Result.success(null);
    }
    
    /**
     * Complete the ingestion process.
     */
    public Result<Void> completeIngestion(String causationId) {
        // Validate specs: must have s3 and exposure count
        Result<Void> spec = BatchSpecifications.mustHaveS3Reference().and(BatchSpecifications.mustHaveExposureCount()).isSatisfiedBy(this);
        if (spec.isFailure()) return spec;

        // Validate transition
        Result<Void> vt = BatchTransitions.validateTransition(this, BatchStatus.COMPLETED);
        if (vt.isFailure()) return vt;

        BatchTransitions.applyTransition(this, BatchStatus.COMPLETED);
        this.completedAt = Instant.now();
        this.processingDurationMs = completedAt.toEpochMilli() - uploadedAt.toEpochMilli();

        BatchProcessingCompletedEvent batchCompletedEvent = new BatchProcessingCompletedEvent(batchId, bankId, s3Reference, totalExposures, fileMetadata.fileSizeBytes(), completedAt, CorrelationContext.correlationId());
        batchCompletedEvent.setCausationId(Maybe.some(causationId));

        addDomainEvent(batchCompletedEvent);

        return Result.success(null);
    }
    
    /**
     * Mark the batch as failed with an error message.
     */
    public void markAsFailed(String errorMessage) {
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        
        // Allow failing from non-terminal states
        Result<Void> vt = BatchTransitions.validateTransition(this, BatchStatus.FAILED);
        if (vt.isFailure()) return;

        BatchTransitions.applyTransition(this, BatchStatus.FAILED);
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        this.processingDurationMs = completedAt.toEpochMilli() - uploadedAt.toEpochMilli();

        Result.success(null);
    }
    
    /**
     * Update the batch status and set updated timestamp.
     */
    /* package-private */ void updateStatus(BatchStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }


    /**
     * Check if the batch is in a terminal state.
     */
    public boolean isTerminal() {
        return status == BatchStatus.COMPLETED || status == BatchStatus.FAILED;
    }

    /**
     * Check if the batch processing was successful.
     */
    public boolean isSuccessful() {
        return status == BatchStatus.COMPLETED;
    }

}

