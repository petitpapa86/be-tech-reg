package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Domain event raised when a batch is uploaded.
 */
public class BatchUploadedEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final FileMetadata fileMetadata;
    private final Instant uploadedAt;
    
    @JsonCreator
    public BatchUploadedEvent(
            @JsonProperty("batchId") BatchId batchId, 
            @JsonProperty("bankId") BankId bankId,
            @JsonProperty("fileMetadata") FileMetadata fileMetadata,
            @JsonProperty("uploadedAt") Instant uploadedAt, 
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId);
        this.batchId = batchId;
        this.bankId = bankId;
        this.fileMetadata = fileMetadata;
        this.uploadedAt = uploadedAt;
    }
    
    @JsonProperty("batchId")
    public BatchId batchId() { return batchId; }
    
    @JsonProperty("bankId")
    public BankId bankId() { return bankId; }
    
    @JsonProperty("fileMetadata")
    public FileMetadata fileMetadata() { return fileMetadata; }

    @JsonProperty("uploadedAt")
    public Instant uploadedAt() { return uploadedAt; }
}
