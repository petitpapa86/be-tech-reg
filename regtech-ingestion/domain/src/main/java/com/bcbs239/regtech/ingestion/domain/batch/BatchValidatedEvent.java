package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;

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