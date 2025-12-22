package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Domain event raised when batch is successfully stored in S3.
 */
public class BatchStoredEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final S3Reference s3Reference;
    private final Instant storedAt;
    
    @JsonCreator
    public BatchStoredEvent(
            @JsonProperty("batchId") BatchId batchId, 
            @JsonProperty("bankId") BankId bankId, 
            @JsonProperty("s3Reference") S3Reference s3Reference, 
            @JsonProperty("storedAt") Instant storedAt, 
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId);
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3Reference = s3Reference;
        this.storedAt = storedAt;
    }

    // Getters with JsonProperty for proper serialization
    @JsonProperty("batchId")
    public BatchId batchId() { return batchId; }
    
    @JsonProperty("bankId")
    public BankId bankId() { return bankId; }
    
    @JsonProperty("s3Reference")
    public S3Reference s3Reference() { return s3Reference; }
    
    @JsonProperty("storedAt")
    public Instant storedAt() { return storedAt; }
}

