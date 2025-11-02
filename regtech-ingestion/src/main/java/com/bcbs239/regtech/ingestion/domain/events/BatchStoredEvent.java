package com.bcbs239.regtech.ingestion.domain.events;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.S3Reference;

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