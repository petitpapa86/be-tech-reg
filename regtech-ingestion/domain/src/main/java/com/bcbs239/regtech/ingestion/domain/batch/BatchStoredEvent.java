package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;

import java.time.Instant;

/**
 * Domain event raised when batch is successfully stored in S3.
 */
public class BatchStoredEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final S3Reference s3Reference;
    private final Instant storedAt;
    
    public BatchStoredEvent(BatchId batchId, BankId bankId, S3Reference s3Reference, Instant storedAt, String correlationId) {
        super(correlationId, "BatchStoredEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3Reference = s3Reference;
        this.storedAt = storedAt;
    }
    
    @Override
    public String eventType() {
        return getEventType();
    }
    
    // Getters
    public BatchId batchId() { return batchId; }
    public BankId bankId() { return bankId; }
    public S3Reference s3Reference() { return s3Reference; }
    public Instant storedAt() { return storedAt; }
}

