package com.bcbs239.regtech.ingestion.domain.events;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BankId;

import java.time.Instant;

/**
 * Domain event raised when batch processing starts.
 */
public record BatchProcessingStartedEvent(
    BatchId batchId,
    BankId bankId,
    Instant startedAt
) implements DomainEvent {
    
    @Override
    public String eventType() {
        return "BatchProcessingStarted";
    }
}