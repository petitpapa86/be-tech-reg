package com.bcbs239.regtech.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class CrossModuleEventBus {

    private static final Logger logger = LoggerFactory.getLogger(CrossModuleEventBus.class);

    private final ApplicationEventPublisher eventPublisher;

    public CrossModuleEventBus(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEvent(Object event) {
        // Using Java 21+ virtual threads for efficient async event publishing
        Thread.ofVirtual().start(() -> {
            try {
                logger.info("📤 ASYNC Publishing cross-module event: {} with data: {}", event.getClass().getSimpleName(), event);
                eventPublisher.publishEvent(event);
                logger.debug("✅ Successfully published cross-module event: {}", event.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("❌ Failed to publish cross-module event: {} - {}", event.getClass().getSimpleName(), e.getMessage(), e);
            }
        });
    }

    public void publishEventSynchronously(Object event) {
        logger.info("📤 SYNC Publishing cross-module event: {} with data: {}", event.getClass().getSimpleName(), event);
        eventPublisher.publishEvent(event);
    }
}