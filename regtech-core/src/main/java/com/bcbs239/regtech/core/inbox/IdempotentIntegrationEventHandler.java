package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import com.bcbs239.regtech.core.events.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdempotentIntegrationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(IdempotentIntegrationEventHandler.class);

    private final InboxMessageJpaRepository inboxRepository;
    private final ObjectMapper objectMapper;

    public IdempotentIntegrationEventHandler(
            InboxMessageJpaRepository inboxRepository,
            ObjectMapper objectMapper) {
        this.inboxRepository = inboxRepository;
        this.objectMapper = objectMapper;
        logger.info("🚀 IdempotentIntegrationEventHandler initialized with shared inbox");
    }

    /**
     * Generic event listener for any IntegrationEvent.
     * Stores the event in the shared inbox for reliable processing.
     */
    @EventListener
    @Transactional
    public void consumeIntegrationEvent(IntegrationEvent event) throws JsonProcessingException {
        String eventType = event.getClass().getName();
        logger.info("📨 Consuming IntegrationEvent: type={}, id={}",
            eventType, event.getId());

        try {
            // Serialize event to JSON
            String eventData = objectMapper.writeValueAsString(event);

            // Determine aggregateId based on event type
            String aggregateId = determineAggregateId(event);

            // Create inbox message entity
            InboxMessageEntity inboxMessage = new InboxMessageEntity(
                eventType,
                eventData,
                aggregateId
            );

            // Save to shared inbox
            inboxRepository.save(inboxMessage);

            logger.info("✅ Stored {} event in shared inbox: id={}", eventType, event.getId());

        } catch (Exception e) {
            logger.error("❌ Failed to store {} event in shared inbox: {}", eventType, e.getMessage(), e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    /**
     * Determines the aggregate ID for the event based on its type.
     * This helps with correlation and deduplication.
     */
    private String determineAggregateId(IntegrationEvent event) {
        // Use reflection or instanceof checks to determine aggregate ID
        // This is a generic implementation that can be extended
        if (event.getClass().getSimpleName().contains("User")) {
            // For user-related events, try to extract userId
            try {
                var userIdMethod = event.getClass().getMethod("getUserId");
                var userId = userIdMethod.invoke(event);
                return userId.toString();
            } catch (Exception e) {
                // Fall back to event ID
            }
        }

        // Default to event ID
        return event.getId().toString();
    }
}
