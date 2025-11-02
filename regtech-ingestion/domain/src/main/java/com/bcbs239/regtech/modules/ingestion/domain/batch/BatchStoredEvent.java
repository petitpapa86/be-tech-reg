package com.bcbs239.regtech.modules.ingestion.domain.batch;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;

import java.time.Instant;

/**
 * Domain event raised when batch is successfully stored in S3.
 */
public record BatchStoredEvent(
    BatchId batchId,
    BankId bankId,
    S3Reference s3Reference,
    Instant storedAt
) implements DomainEvent {
    
    @Override
    public String eventType() {
        return "BatchStored";
    }
}