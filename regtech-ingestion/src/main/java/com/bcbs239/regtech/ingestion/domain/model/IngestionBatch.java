package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.shared.Entity;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.events.BatchProcessingStartedEvent;
import com.bcbs239.regtech.ingestion.domain.events.BatchValidatedEvent;
import com.bcbs239.regtech.ingestion.domain.events.BatchStoredEvent;
import com.bcbs239.regtech.ingestion.domain.events.BatchCompletedEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate root representing an ingestion batch with its complete lifecycle.
 */
@Getter
public class IngestionBatch extends Entity {

    // Getters
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
    
    /**
     * Constructor for creating a new ingestion batch.
     */
    public IngestionBatch(BatchId batchId, BankId bankId, FileMetadata fileMetadata) {
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.bankId = Objects.requireNonNull(bankId, "Bank ID cannot be null");
        this.fileMetadata = Objects.requireNonNull(fileMetadata, "File metadata cannot be null");
        this.status = BatchStatus.UPLOADED;
        this.uploadedAt = Instant.now();
    }
    
    /**
     * Constructor for reconstituting from persistence.
     */
    public IngestionBatch(BatchId batchId, BankId bankId, BatchStatus status, 
                         FileMetadata fileMetadata, S3Reference s3Reference, 
                         BankInfo bankInfo, Integer totalExposures, 
                         Instant uploadedAt, Instant completedAt, 
                         String errorMessage, Long processingDurationMs) {
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
    }
    
    /**
     * Start processing the batch by transitioning to PARSING status.
     */
    public Result<Void> startProcessing() {
        if (!canTransitionTo(BatchStatus.PARSING)) {
            return Result.failure(new ErrorDetail("INVALID_STATE_TRANSITION", 
                String.format("Cannot transition from %s to PARSING", status)));
        }
        
        this.status = BatchStatus.PARSING;
        addDomainEvent(new BatchProcessingStartedEvent(batchId, bankId, Instant.now()));
        
        return Result.success(null);
    }
    
    /**
     * Mark the batch as validated with the exposure count.
     */
    public Result<Void> markAsValidated(int exposureCount) {
        if (!canTransitionTo(BatchStatus.VALIDATED)) {
            return Result.failure(new ErrorDetail("INVALID_STATE_TRANSITION", 
                String.format("Cannot transition from %s to VALIDATED", status)));
        }
        
        if (exposureCount < 0) {
            return Result.failure(new ErrorDetail("INVALID_EXPOSURE_COUNT", 
                "Exposure count cannot be negative"));
        }
        
        this.status = BatchStatus.VALIDATED;
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
        
        if (!canTransitionTo(BatchStatus.STORING)) {
            return Result.failure(new ErrorDetail("INVALID_STATE_TRANSITION", 
                String.format("Cannot transition from %s to STORING", status)));
        }
        
        this.status = BatchStatus.STORING;
        this.s3Reference = s3Reference;
        addDomainEvent(new BatchStoredEvent(batchId, bankId, s3Reference, Instant.now()));
        
        return Result.success(null);
    }
    
    /**
     * Complete the ingestion process.
     */
    public Result<Void> completeIngestion() {
        if (!canTransitionTo(BatchStatus.COMPLETED)) {
            return Result.failure(new ErrorDetail("INVALID_STATE_TRANSITION", 
                String.format("Cannot transition from %s to COMPLETED", status)));
        }
        
        if (s3Reference == null) {
            return Result.failure(new ErrorDetail("MISSING_S3_REFERENCE", 
                "Cannot complete ingestion without S3 reference"));
        }
        
        if (totalExposures == null) {
            return Result.failure(new ErrorDetail("MISSING_EXPOSURE_COUNT", 
                "Cannot complete ingestion without exposure count"));
        }
        
        this.status = BatchStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.processingDurationMs = completedAt.toEpochMilli() - uploadedAt.toEpochMilli();
        
        addDomainEvent(new BatchCompletedEvent(batchId, bankId, s3Reference, 
            totalExposures, fileMetadata.fileSizeBytes(), completedAt));
        
        return Result.success(null);
    }
    
    /**
     * Mark the batch as failed with an error message.
     */
    public Result<Void> markAsFailed(String errorMessage) {
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        
        if (!canTransitionTo(BatchStatus.FAILED)) {
            return Result.failure(new ErrorDetail("INVALID_STATE_TRANSITION", 
                String.format("Cannot transition from %s to FAILED", status)));
        }
        
        this.status = BatchStatus.FAILED;
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
}