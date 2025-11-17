package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Domain event raised when batch validation completes successfully.
 */
public class BatchValidatedEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final int exposureCount;
    private final Instant validatedAt;
    
    @JsonCreator
    public BatchValidatedEvent(
            @JsonProperty("batchId") BatchId batchId, 
            @JsonProperty("bankId") BankId bankId, 
            @JsonProperty("exposureCount") int exposureCount, 
            @JsonProperty("validatedAt") Instant validatedAt, 
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId, "BatchValidatedEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.exposureCount = exposureCount;
        this.validatedAt = validatedAt;
    }
    
    @Override
    public String eventType() {
        return getEventType();
    }
    
    // Getters
    public BatchId batchId() { return batchId; }
    public BankId bankId() { return bankId; }
    public int exposureCount() { return exposureCount; }
    public Instant validatedAt() { return validatedAt; }
}

