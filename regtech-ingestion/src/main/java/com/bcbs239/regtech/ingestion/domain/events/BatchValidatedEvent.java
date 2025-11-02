package com.bcbs239.regtech.ingestion.domain.events;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BankId;

import java.time.Instant;

/**
 * Domain event raised when batch validation completes successfully.
 */
public record BatchValidatedEvent(
    BatchId batchId,
    BankId bankId,
    int exposureCount,
    Instant validatedAt
) implements DomainEvent {
    
    @Override
    public String eventType() {
        return "BatchValidated";
    }
}