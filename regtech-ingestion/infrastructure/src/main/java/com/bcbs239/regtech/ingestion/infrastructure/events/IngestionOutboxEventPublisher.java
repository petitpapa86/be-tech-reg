package com.bcbs239.regtech.ingestion.infrastructure.events;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.bcbs239.regtech.core.events.OutboxEventPublisher;
import com.bcbs239.regtech.core.infrastructure.outbox.OutboxMessageEntity;
import com.bcbs239.regtech.core.infrastructure.outbox.OutboxMessageRepository;
import com.bcbs239.regtech.core.infrastructure.outbox.OutboxMessageStatus;
import com.bcbs239.regtech.ingestion.domain.integrationevents.BatchIngestedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Outbox event publisher for ingestion module events.
 * Implements the transactional outbox pattern to ensure reliable event delivery
 * to downstream modules via the CrossModuleEventBus.
 */
@Component
public class IngestionOutboxEventPublisher implements OutboxEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(IngestionOutboxEventPublisher.class);
    private static final String CONTEXT_NAME = "ingestion";
    
    private final OutboxMessageRepository outboxRepository;
    private final CrossModuleEventBus eventBus;
    private final ObjectMapper objectMapper;
    
    public IngestionOutboxEventPublisher(
            OutboxMessageRepository outboxRepository,
            CrossModuleEventBus eventBus) {
        this.outboxRepository = outboxRepository;
        this.eventBus = eventBus;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * Publishes a BatchIngestedEvent by storing it in the outbox table.
     * The event will be processed asynchronously by the outbox processor.
     */
    @Transactional
    public void publishBatchIngestedEvent(BatchIngestedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            
            OutboxMessageEntity outboxMessage = new OutboxMessageEntity(
                event.getClass().getSimpleName(),
                eventJson,
                event.getOccurredOn().atZone(java.time.ZoneOffset.UTC).toInstant()
            );
            
            outboxRepository.save(outboxMessage);
            
            logger.info("Stored BatchIngestedEvent in outbox", 
                LoggingConfiguration.createStructuredLog("BATCH_INGESTED_EVENT_STORED", Map.of(
                    "batchId", event.getBatchId(),
                    "bankId", event.getBankId(),
                    "totalExposures", event.getTotalExposures(),
                    "outboxMessageId", outboxMessage.getId()
                )));
                
        } catch (JsonProcessingException e) {
            LoggingConfiguration.logError("event_serialization", "SERIALIZATION_FAILED", 
                "Failed to serialize BatchIngestedEvent", e, Map.of(
                    "batchId", event.getBatchId(),
                    "eventType", event.getClass().getSimpleName()
                ));
            throw new RuntimeException("Failed to serialize event for outbox storage", e);
        }
    }
    
    @Override
    @Transactional
    public void processPendingEvents() {
        List<OutboxMessageEntity> pendingEvents = outboxRepository.findByStatusOrderByOccurredOnUtc(
            OutboxMessageStatus.PENDING);
        
        logger.debug("Processing {} pending outbox events", pendingEvents.size());
        
        for (OutboxMessageEntity outboxMessage : pendingEvents) {
            try {
                // Mark as processing to prevent duplicate processing
                outboxMessage.setStatus(OutboxMessageStatus.PROCESSING);
                outboxRepository.save(outboxMessage);
                
                // Deserialize and publish the event
                Object event = deserializeEvent(outboxMessage);
                eventBus.publish((com.bcbs239.regtech.core.application.IntegrationEvent) event);
                
                // Mark as processed
                outboxMessage.markAsProcessed();
                outboxRepository.save(outboxMessage);
                
                logger.debug("Successfully processed outbox event: {}", outboxMessage.getId());
                
            } catch (Exception e) {
                // Mark as failed
                outboxMessage.markAsFailed(e.getMessage());
                outboxRepository.save(outboxMessage);
                
                LoggingConfiguration.logError("outbox_processing", "EVENT_PROCESSING_FAILED",
                    "Failed to process outbox event", e, Map.of(
                        "outboxMessageId", outboxMessage.getId(),
                        "eventType", outboxMessage.getType()
                    ));
            }
        }
    }
    
    @Override
    @Transactional
    public void retryFailedEvents(int maxRetries) {
        List<OutboxMessageEntity> failedEvents = outboxRepository.findByStatusOrderByOccurredOnUtc(
            OutboxMessageStatus.FAILED);
        
        logger.debug("Retrying {} failed outbox events", failedEvents.size());
        
        for (OutboxMessageEntity outboxMessage : failedEvents) {
            try {
                // Reset to pending for retry
                outboxMessage.setStatus(OutboxMessageStatus.PENDING);
                outboxRepository.save(outboxMessage);
                
                logger.debug("Reset failed event to pending for retry: {}", outboxMessage.getId());
                
            } catch (Exception e) {
                LoggingConfiguration.logError("outbox_retry", "RETRY_RESET_FAILED",
                    "Failed to reset failed event for retry", e, Map.of(
                        "outboxMessageId", outboxMessage.getId(),
                        "eventType", outboxMessage.getType()
                    ));
            }
        }
    }
    
    @Override
    public OutboxEventStats getStats() {
        long pending = outboxRepository.countByStatus(OutboxMessageStatus.PENDING);
        long processing = outboxRepository.countByStatus(OutboxMessageStatus.PROCESSING);
        long processed = outboxRepository.countByStatus(OutboxMessageStatus.PROCESSED);
        long failed = outboxRepository.countByStatus(OutboxMessageStatus.FAILED);
        
        return new OutboxEventStats(pending, processing, processed, failed, 0L);
    }
    
    private Object deserializeEvent(OutboxMessageEntity outboxMessage) throws JsonProcessingException {
        String eventType = outboxMessage.getType();
        String content = outboxMessage.getContent();
        
        return switch (eventType) {
            case "BatchIngestedEvent" -> objectMapper.readValue(content, BatchIngestedEvent.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}

