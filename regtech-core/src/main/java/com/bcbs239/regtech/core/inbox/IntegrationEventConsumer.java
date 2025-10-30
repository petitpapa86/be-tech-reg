package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * Generic integration event consumer that handles any event extending IntegrationEvent.
 * Stores incoming integration events in the shared inbox for reliable processing.
 * This is shared infrastructure used by all bounded contexts.
 */
@Component
public class IntegrationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventConsumer.class);

    private final Function<InboxMessageEntity, InboxMessageEntity> saveFn;
    private final ObjectMapper objectMapper;

    public IntegrationEventConsumer(
            Function<InboxMessageEntity, InboxMessageEntity> saveFn,
            ObjectMapper objectMapper) {
        this.saveFn = saveFn;
        this.objectMapper = objectMapper;
        logger.info("🚀 IntegrationEventConsumer initialized with shared inbox");
    }

    /**
     * Generic event listener for any IntegrationEvent.
     * Stores the event in the shared inbox for reliable processing.
     */
    @EventListener
    @Transactional
    public void consumeIntegrationEvent(IntegrationEvent event) throws JsonProcessingException {
        String eventType = event.getClass().getName();
        String eventId = event.getId().toString();
        logger.info("📨 Consuming IntegrationEvent: type={}, id={}",
            eventType, event.getId());

        try {
            // Serialize event to JSON
            String eventData = objectMapper.writeValueAsString(event);

            // Determine aggregateId based on event type
            String aggregateId = determineAggregateId(event);

            // Create inbox message entity including eventId for idempotency
            InboxMessageEntity inboxMessage = new InboxMessageEntity(
                eventId,
                eventType,
                eventData,
                aggregateId
            );

            // Save to shared inbox (saveFn now enforces idempotency by eventId)
            InboxMessageEntity saved = saveFn.apply(inboxMessage);

            if (saved != null && saved.getId() != null && !saved.getId().equals(inboxMessage.getId())) {
                logger.info("ℹ️ Event already exists in inbox, skipping duplicate: eventId={}", eventId);
            } else {
                logger.info("✅ Stored {} event in shared inbox: id={}", eventType, event.getId());
            }

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

    /**
     * Generic event listener for any ApplicationEvent.
     * Can be extended to handle Spring ApplicationEvents if needed.
     */
    @EventListener
    public void consumeApplicationEvent(Object event) {
        // Only log non-IntegrationEvent objects to avoid duplicate logging
        if (!(event instanceof IntegrationEvent)) {
            logger.debug("📨 Consuming ApplicationEvent: {}", event.getClass().getSimpleName());
        }
    }
}