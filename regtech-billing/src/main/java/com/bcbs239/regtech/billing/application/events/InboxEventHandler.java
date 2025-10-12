package com.bcbs239.regtech.billing.application.events;

import com.bcbs239.regtech.billing.infrastructure.database.entities.InboxEventEntity;
import com.bcbs239.regtech.core.events.BaseEvent;
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

    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final java.util.function.Consumer<InboxEventEntity> inboxSaver;

    public InboxEventHandler(ObjectMapper objectMapper,
                             TransactionTemplate transactionTemplate,
                             java.util.function.Consumer<InboxEventEntity> inboxSaver) {
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.inboxSaver = inboxSaver;
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
     * Generic listener for integration events coming from other bounded contexts.
     * Stores events in the billing inbox table for reliable asynchronous processing.
     */
    @EventListener
    public void handleExternalIntegrationEvent(Object event) {
        // Only handle BaseEvent types (our integration events extend BaseEvent)
        if (!(event instanceof BaseEvent be)) {
            logger.debug("Ignoring non-BaseEvent type: {}", event.getClass().getSimpleName());
            return;
        }

        // Avoid storing events emitted by this module
        if ("billing".equalsIgnoreCase(be.getSourceModule())) {
            logger.debug("Ignoring event from same module: {}", be.getSourceModule());
            return;
        }

        String eventType = be.eventType();
        String aggregateId = extractAggregateId(event);

        logger.info("📨 Storing inbound integration event: type={}, aggregateId={}, correlation={}",
            eventType, aggregateId, be.getCorrelationId());

        try {
            logger.debug("InboxEventHandler: entering transaction to persist event {}", eventType);
            transactionTemplate.execute(status -> {
                try {
                    String eventData = objectMapper.writeValueAsString(event);

                    InboxEventEntity inboxEvent = new InboxEventEntity(eventType, aggregateId, eventData);
                    inboxSaver.accept(inboxEvent);
                    logger.info("Stored inbox event id={} type={} aggregate={}", inboxEvent.getId(), eventType, aggregateId);
                    logger.debug("InboxEventHandler: after inboxSaver.accept, id={}", inboxEvent.getId());
                    return null;
                } catch (Exception e) {
                    logger.error("Failed to persist inbox event type={} aggregate={}: {}",
                        eventType, aggregateId, e.getMessage(), e);
                    status.setRollbackOnly();
                    return null;
                }
            });
            logger.debug("InboxEventHandler: transaction complete for event {}", eventType);
        } catch (Exception e) {
            logger.error("Unexpected error storing inbound event type={} aggregate={}: {}",
                eventType, aggregateId, e.getMessage(), e);
        }
    }

    /**
     * Try to extract an aggregate id from the incoming event using common getter names.
     */
    private String extractAggregateId(Object event) {
        try {
            java.lang.reflect.Method m;

            // common getter names to try
            String[] candidates = {"getAggregateId", "getUserId", "getId", "getAccountId", "getCustomerId"};
            for (String name : candidates) {
                try {
                    m = event.getClass().getMethod(name);
                    Object val = m.invoke(event);
                    if (val != null) return val.toString();
                } catch (NoSuchMethodException ignored) {
                    // try next
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to extract aggregate id: {}", e.getMessage());
        }
        return null;
    }
}