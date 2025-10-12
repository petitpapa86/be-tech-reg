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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    public InboxEventHandler(ObjectMapper objectMapper, TransactionTemplate transactionTemplate) {
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        logger.info("🚀 InboxEventHandler initialized and ready to receive events");
    }

    /**
     * Generic event listener to debug all incoming events
     */
    @EventListener
    public void handleAllEvents(Object event) {
        logger.debug("📨 RECEIVED GENERIC EVENT in billing: {} - {}", event.getClass().getSimpleName(), event);
    }

    /**
     * Handle UserRegisteredIntegrationEvent by storing it in the inbox for asynchronous processing.
     */
    @EventListener
    public void handleUserRegisteredEvent(UserRegisteredIntegrationEvent event) {
        logger.info("📨 RECEIVED UserRegisteredIntegrationEvent in billing context: user={}, bank={}, correlation={}",
            event.getUserId(), event.getBankId(), event.getCorrelationId());

        try {
            // Use transaction template for database operations
            transactionTemplate.execute(status -> {
                try {
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

                    return null;
                } catch (Exception e) {
                    logger.error("Failed to store UserRegisteredEvent in inbox for user {}: {}",
                        event.getUserId(), e.getMessage(), e);
                    status.setRollbackOnly();
                    return null;
                }
            });

        } catch (Exception e) {
            logger.error("Unexpected error in event handling for user {}: {}",
                event.getUserId(), e.getMessage(), e);
        }
    }
}