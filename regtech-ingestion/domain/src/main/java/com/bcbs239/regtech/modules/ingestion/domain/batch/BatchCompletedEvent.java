package com.bcbs239.regtech.modules.ingestion.domain.batch;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;

import java.time.Instant;

/**
 * Domain event raised when batch processing completes successfully.
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