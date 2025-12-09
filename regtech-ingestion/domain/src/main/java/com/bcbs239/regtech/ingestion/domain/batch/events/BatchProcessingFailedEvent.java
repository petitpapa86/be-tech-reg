package com.bcbs239.regtech.ingestion.domain.batch.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import lombok.Getter;

/**
 * Domain event published when batch processing fails.
 * This event triggers the creation of an EventProcessingFailure record for retry.
 */
@Getter
public class BatchProcessingFailedEvent extends DomainEvent {
    
    private final String batchId;
    private final String bankId;
    private final String fileName;
    private final String errorMessage;
    private final String errorType;
    private final String tempFileKey;
    
    public BatchProcessingFailedEvent(
            String batchId,
            String bankId,
            String fileName,
            String errorMessage,
            String errorType,
            String tempFileKey) {
        super(java.util.UUID.randomUUID().toString(), "BatchProcessingFailedEvent");
        this.batchId = batchId;
        this.bankId = bankId;
        this.fileName = fileName;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.tempFileKey = tempFileKey;
    }

    @Override
    public String eventType() {
        return this.getClass().getSimpleName();
    }
}
