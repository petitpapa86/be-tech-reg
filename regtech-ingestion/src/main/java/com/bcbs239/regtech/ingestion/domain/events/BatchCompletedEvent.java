package com.bcbs239.regtech.ingestion.domain.events;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.S3Reference;

import java.time.Instant;

/**
 * Domain event raised when batch ingestion completes successfully.
 */
public record BatchCompletedEvent(
    BatchId batchId,
    BankId bankId,
    S3Reference s3Reference,
    int totalExposures,
    long fileSizeBytes,
    Instant completedAt
) implements DomainEvent {
    
    @Override
    public String eventType() {
        return "BatchCompleted";
    }
}