package com.bcbs239.regtech.billing.infrastructure.inbox;

import com.bcbs239.regtech.core.events.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;

/**
 * Generic integration event consumer that handles any event extending BaseEvent or ApplicationEvent.
 * Provides a unified entry point for all cross-module integration events in the billing context.
 * Routes events directly to domain handlers for business logic processing.
 */
@Component
public class IntegrationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationEventConsumer.class);

    private final Map<String, Function<BaseEvent, Boolean>> handlers;

    public IntegrationEventConsumer(@Qualifier("billingInboxHandlers") Map<String, Function<BaseEvent, Boolean>> handlers) {
        this.handlers = handlers == null ? Map.of() : handlers;
        logger.info("🚀 IntegrationEventConsumer initialized with {} handlers", this.handlers.size());
    }

    /**
     * Generic event listener for any BaseEvent.
     * Routes events to appropriate handlers based on event type.
     */
    @EventListener
    @Transactional
    public void consumeBaseEvent(BaseEvent event) {
        String eventType = event.getClass().getSimpleName();
        logger.info("📨 Consuming BaseEvent: type={}, source={}, correlation={}",
            eventType, event.getSourceModule(), event.getCorrelationId());

        try {
            Function<BaseEvent, Boolean> handler = handlers.get(eventType);

            if (handler != null) {
                boolean success = handler.apply(event);
                if (success) {
                    logger.info("✅ Successfully processed {} event", eventType);
                } else {
                    logger.warn("❌ Handler returned false for {} event", eventType);
                }
            } else {
                logger.warn("No handler configured for BaseEvent type: {}", eventType);
            }
        } catch (Exception e) {
            logger.error("Failed to consume BaseEvent {}: {}", eventType, e.getMessage(), e);
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
