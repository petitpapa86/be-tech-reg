package com.bcbs239.regtech.ingestion.infrastructure.events;

import com.bcbs239.regtech.core.events.GenericOutboxEventProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Scheduled processor for ingestion module outbox events.
 * Extends GenericOutboxEventProcessor to leverage existing retry and monitoring infrastructure.
 * 
 * Processing is enabled by default but can be disabled via configuration property:
 * regtech.ingestion.outbox.processing.enabled=false
 */
@Component
@ConditionalOnProperty(
    name = "regtech.ingestion.outbox.processing.enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public class IngestionOutboxProcessor extends GenericOutboxEventProcessor {
    
    private static final String CONTEXT_NAME = "ingestion";
    private static final int DEFAULT_MAX_RETRIES = 3;
    
    private final boolean processingEnabled;
    
    public IngestionOutboxProcessor(
            IngestionOutboxEventPublisher eventPublisher,
            @Value("${regtech.ingestion.outbox.processing.enabled:true}") boolean processingEnabled,
            @Value("${regtech.ingestion.outbox.max-retries:3}") int maxRetries) {
        super(eventPublisher, CONTEXT_NAME, maxRetries);
        this.processingEnabled = processingEnabled;
    }
    
    @Override
    public boolean isProcessingEnabled() {
        return processingEnabled;
    }
}