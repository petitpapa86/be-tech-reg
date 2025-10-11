package com.bcbs239.regtech.billing.application.events;

import com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inbox event handler that receives cross-module events and stores them for asynchronous processing.
 * This implements the inbox pattern for reliable event processing.
 */
@Component
public class InboxEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(InboxEventHandler.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public InboxEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Handle UserRegisteredIntegrationEvent by storing it in the inbox for asynchronous processing.
     */
    @EventListener
    @Transactional
    public void handleUserRegisteredEvent(UserRegisteredIntegrationEvent event) {
        try {
            logger.info("Received UserRegisteredIntegrationEvent for inbox processing: user={}, bank={}",
                event.getUserId(), event.getBankId());

            // Serialize the event
            String eventData = objectMapper.writeValueAsString(event);

            // Create inbox event entity
            InboxEventEntity inboxEvent = new InboxEventEntity(
                "UserRegisteredIntegrationEvent",
                event.getUserId(), // aggregateId is the userId
                eventData
            );

            // Save to inbox
            entityManager.persist(inboxEvent);

            logger.info("Stored UserRegisteredEvent in inbox: id={}, user={}",
                inboxEvent.getId(), event.getUserId());

        } catch (Exception e) {
            logger.error("Failed to store UserRegisteredEvent in inbox for user {}: {}",
                event.getUserId(), e.getMessage(), e);
            // In a real system, you might want to send this to a dead letter queue
            // or implement retry logic here
        }
    }
}