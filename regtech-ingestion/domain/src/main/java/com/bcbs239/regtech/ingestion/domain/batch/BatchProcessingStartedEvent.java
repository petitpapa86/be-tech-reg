package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Domain event raised when batch processing starts.
 */
public class BatchProcessingStartedEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant startedAt;
    
    @JsonCreator
    public BatchProcessingStartedEvent(
            @JsonProperty("batchId") BatchId batchId, 
            @JsonProperty("bankId") BankId bankId, 
            @JsonProperty("startedAt") Instant startedAt, 
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId, "BatchProcessingStartedEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.startedAt = startedAt;
    }
    
    @Override
    public String eventType() {
        return getEventType();
    }
    
    // Getters
    public BatchId batchId() { return batchId; }
    public BankId bankId() { return bankId; }
    public Instant startedAt() { return startedAt; }
}

