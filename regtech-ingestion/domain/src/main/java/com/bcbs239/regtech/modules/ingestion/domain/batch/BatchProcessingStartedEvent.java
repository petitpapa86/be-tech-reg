package com.bcbs239.regtech.modules.ingestion.domain.batch;

import com.bcbs239.regtech.core.shared.DomainEvent;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;

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
    public Instant occurredOn() {
        return startedAt;
    }
}