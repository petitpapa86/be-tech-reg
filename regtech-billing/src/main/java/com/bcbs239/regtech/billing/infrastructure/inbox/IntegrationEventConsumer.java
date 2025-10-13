package com.bcbs239.regtech.billing.infrastructure.inbox;

import com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity;
import com.bcbs239.regtech.core.events.BaseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * Generic integration event consumer that handles any event extending BaseEvent.
 * Stores incoming integration events in the inbox for reliable processing by the inbox processor.
 */
@Component
public class IntegrationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventConsumer.class);

    private final Consumer<InboxEventEntity> inboxSaver;
    private final ObjectMapper objectMapper;

    public IntegrationEventConsumer(
            @Qualifier("billingInboxSaver") Consumer<InboxEventEntity> inboxSaver,
            ObjectMapper objectMapper) {
        this.inboxSaver = inboxSaver;
        this.objectMapper = objectMapper;
        logger.info("🚀 IntegrationEventConsumer initialized");
    }

    /**
     * Generic event listener for any BaseEvent.
     * Stores the event in the inbox for reliable processing.
     */
    @EventListener
    @Transactional
    public void consumeBaseEvent(BaseEvent event) throws JsonProcessingException {
        String eventType = event.getClass().getSimpleName();
        logger.info("📨 Consuming BaseEvent: type={}, source={}, correlation={}",
            eventType, event.getSourceModule(), event.getCorrelationId());

        try {
            // Serialize event to JSON
            String eventData = objectMapper.writeValueAsString(event);

            // Create inbox event entity
            InboxEventEntity inboxEvent = new InboxEventEntity(
                eventType,
                event.getCorrelationId(), // Use correlationId as aggregateId for integration events
                eventData
            );

            // Save to inbox
            inboxSaver.accept(inboxEvent);

            logger.info("✅ Stored {} event in inbox: correlationId={}", eventType, event.getCorrelationId());

        } catch (Exception e) {
            logger.error("❌ Failed to store {} event in inbox: {}", eventType, e.getMessage(), e);
            throw e; // Re-throw to trigger transaction rollback
        }
    }

    // Specific typed handlers are registered in the handlers map by `BillingInboxWiring`.

    /**
     * Generic event listener for any ApplicationEvent.
     * Can be extended to handle Spring ApplicationEvents if needed.
     */
    @EventListener
    public void consumeApplicationEvent(Object event) {
        // Only log non-BaseEvent objects to avoid duplicate logging
        if (!(event instanceof BaseEvent)) {
            logger.debug("📨 Consuming ApplicationEvent: {}", event.getClass().getSimpleName());
        }
    }
}
