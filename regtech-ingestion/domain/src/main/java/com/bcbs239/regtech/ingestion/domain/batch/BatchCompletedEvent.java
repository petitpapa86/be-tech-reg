package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;

import java.time.Instant;

/**
 * Domain event raised when batch processing completes successfully.
 */
public class BatchCompletedEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final S3Reference s3Reference;
    private final int totalExposures;
    private final long fileSizeBytes;
    private final Instant completedAt;
    
    public BatchCompletedEvent(BatchId batchId, BankId bankId, S3Reference s3Reference, 
                              int totalExposures, long fileSizeBytes, Instant completedAt, 
                              String correlationId) {
        super(correlationId, "BatchCompletedEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3Reference = s3Reference;
        this.totalExposures = totalExposures;
        this.fileSizeBytes = fileSizeBytes;
        this.completedAt = completedAt;
    }
    
    @Override
    public String eventType() {
        return getEventType();
    }
    
    // Getters
    public BatchId batchId() { return batchId; }
    public BankId bankId() { return bankId; }
    public S3Reference s3Reference() { return s3Reference; }
    public int totalExposures() { return totalExposures; }
    public long fileSizeBytes() { return fileSizeBytes; }
    public Instant completedAt() { return completedAt; }
}

