package com.bcbs239.regtech.core.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Implementation of IIntegrationEventBus that publishes integration events
 * to the Spring ApplicationEventPublisher for cross-module communication.
 * Events are consumed by IdempotentIntegrationEventHandler and stored in the shared inbox.
 */
@Component
public class SpringIntegrationEventBus implements IIntegrationEventBus {

    private static final Logger logger = LoggerFactory.getLogger(SpringIntegrationEventBus.class);

    private final ApplicationEventPublisher eventPublisher;

    public SpringIntegrationEventBus(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        logger.info("🚀 SpringIntegrationEventBus initialized for cross-module event publishing");
    }

    @Override
    public void publish(IntegrationEvent event) {
        logger.info("📤 Publishing integration event: {} with id={}",
            event.getEventType(), event.getId());

        try {
            eventPublisher.publishEvent(event);
            logger.debug("✅ Successfully published integration event: {}", event.getEventType());
        } catch (Exception e) {
            logger.error("❌ Failed to publish integration event: {} - {}",
                event.getEventType(), e.getMessage(), e);
            throw e; // Re-throw to let caller handle
        }
    }
}