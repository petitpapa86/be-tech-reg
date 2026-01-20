package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain event raised when batch processing completes successfully.
 */
public class BatchProcessingCompletedEvent extends DomainEvent {
    
    private final BatchId batchId;
    private final BankId bankId;
    private final S3Reference s3Reference;
    private final int totalExposures;
    private final long fileSizeBytes;
    private final Instant completedAt;
    private final String fileName;
    
    @JsonCreator
    public BatchProcessingCompletedEvent(
            @JsonProperty("batchId") BatchId batchId, 
            @JsonProperty("bankId") BankId bankId, 
            @JsonProperty("s3Reference") S3Reference s3Reference, 
            @JsonProperty("totalExposures") int totalExposures, 
            @JsonProperty("fileSizeBytes") long fileSizeBytes, 
            @JsonProperty("completedAt") Instant completedAt, 
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("fileName") String fileName){
        super(correlationId);
        this.batchId = Objects.requireNonNull(batchId, "batchId cannot be null");
        this.bankId = Objects.requireNonNull(bankId, "bankId cannot be null");
        this.s3Reference = Objects.requireNonNull(s3Reference, "s3Reference cannot be null");
        this.totalExposures = totalExposures;
        this.fileSizeBytes = fileSizeBytes;
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "fileName cannot be null");
    }

    
    // Getters with JsonProperty for proper serialization
    @JsonProperty("batchId")
    public BatchId batchId() { return batchId; }
    
    @JsonProperty("bankId")
    public BankId bankId() { return bankId; }
    
    @JsonProperty("s3Reference")
    public S3Reference s3Reference() { return s3Reference; }
    
    @JsonProperty("totalExposures")
    public int totalExposures() { return totalExposures; }
    
    @JsonProperty("fileSizeBytes")
    public long fileSizeBytes() { return fileSizeBytes; }
    
    @JsonProperty("completedAt")
    public Instant completedAt() { return completedAt; }
    @JsonProperty("fileName")
    public String fileName() { return fileName;}
}

