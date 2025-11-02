package com.bcbs239.regtech.modules.ingestion.domain.batch;

import com.bcbs239.regtech.core.shared.Entity;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankInfo;
import com.bcbs239.regtech.modules.ingestion.domain.batch.rules.BatchSpecifications;
import com.bcbs239.regtech.modules.ingestion.domain.batch.rules.BatchTransitions;
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
        addDomainEvent(new BatchProcessingStartedEvent(batchId, bankId, Instant.now()));

        return Result.success(null);
    }
    
    /**
     * Mark the batch as validated with the exposure count.
     */
    public Result<Void> markAsValidated(int exposureCount) {
        // Validate exposure count
        if (exposureCount < 0) {
            return Result.failure(new ErrorDetail("INVALID_EXPOSURE_COUNT", "Exposure count cannot be negative"));
        }

        // Transition validation
        Result<Void> vt = BatchTransitions.validateTransition(this, BatchStatus.VALIDATED);
        if (vt.isFailure()) return vt;

        // Apply
        BatchTransitions.applyTransition(this, BatchStatus.VALIDATED);
        this.totalExposures = exposureCount;
        addDomainEvent(new BatchValidatedEvent(batchId, bankId, exposureCount, Instant.now()));

        return Result.success(null);
    }
    
    /**
     * Attach bank information to the batch.
     */
    public Result<Void> attachBankInfo(BankInfo bankInfo) {
        Objects.requireNonNull(bankInfo, "Bank info cannot be null");
        
        if (!bankInfo.bankId().equals(this.bankId)) {
            return Result.failure(new ErrorDetail("BANK_ID_MISMATCH", 
                "Bank info does not match batch bank ID"));
        }
        
        if (!bankInfo.isActive()) {
            return Result.failure(new ErrorDetail("BANK_INACTIVE", 
                "Cannot process batch for inactive bank"));
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
        addDomainEvent(new BatchStoredEvent(batchId, bankId, s3Reference, Instant.now()));

        return Result.success(null);
    }
    
    /**
     * Complete the ingestion process.
     */
    public Result<Void> completeIngestion() {
        // Validate specs: must have s3 and exposure count
        Result<Void> spec = BatchSpecifications.mustHaveS3Reference().and(BatchSpecifications.mustHaveExposureCount()).isSatisfiedBy(this);
        if (spec.isFailure()) return spec;

        // Validate transition
        Result<Void> vt = BatchTransitions.validateTransition(this, BatchStatus.COMPLETED);
        if (vt.isFailure()) return vt;

        BatchTransitions.applyTransition(this, BatchStatus.COMPLETED);
        this.completedAt = Instant.now();
        this.processingDurationMs = completedAt.toEpochMilli() - uploadedAt.toEpochMilli();

        addDomainEvent(new BatchCompletedEvent(batchId, bankId, s3Reference, totalExposures, fileMetadata.fileSizeBytes(), completedAt));

        return Result.success(null);
    }
    
    /**
     * Mark the batch as failed with an error message.
     */
    public Result<Void> markAsFailed(String errorMessage) {
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        
        // Allow failing from non-terminal states
        Result<Void> vt = BatchTransitions.validateTransition(this, BatchStatus.FAILED);
        if (vt.isFailure()) return vt;

        BatchTransitions.applyTransition(this, BatchStatus.FAILED);
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        this.processingDurationMs = completedAt.toEpochMilli() - uploadedAt.toEpochMilli();

        return Result.success(null);
    }
    
    /**
     * Check if the batch can transition to the specified status.
     */
    private boolean canTransitionTo(BatchStatus newStatus) {
        return switch (status) {
            case UPLOADED -> newStatus == BatchStatus.PARSING || newStatus == BatchStatus.FAILED;
            case PARSING -> newStatus == BatchStatus.VALIDATED || newStatus == BatchStatus.FAILED;
            case VALIDATED -> newStatus == BatchStatus.STORING || newStatus == BatchStatus.FAILED;
            case STORING -> newStatus == BatchStatus.COMPLETED || newStatus == BatchStatus.FAILED;
            case COMPLETED, FAILED -> false; // Terminal states
        };
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
    
    /**
     * Check if the batch processing failed.
     */
    public boolean isFailed() {
        return status == BatchStatus.FAILED;
    }
    
    /**
     * Update the batch status and set updated timestamp.
     */
    public void updateStatus(BatchStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Clear the error message (used during recovery).
     */
    public void clearErrorMessage() {
        this.errorMessage = null;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Increment recovery attempts counter.
     */
    public void incrementRecoveryAttempts() {
        this.recoveryAttempts++;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Set the last checkpoint timestamp.
     */
    public void setLastCheckpoint(Instant checkpoint) {
        this.lastCheckpoint = checkpoint;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Set checkpoint data for recovery purposes.
     */
    public void setCheckpointData(String data) {
        this.checkpointData = data;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Set the completed timestamp.
     */
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        this.updatedAt = Instant.now();
    }
}

