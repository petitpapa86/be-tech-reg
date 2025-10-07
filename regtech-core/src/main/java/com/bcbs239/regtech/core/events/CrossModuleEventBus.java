package com.bcbs239.regtech.core.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class CrossModuleEventBus {

    private static final Logger logger = LoggerFactory.getLogger(CrossModuleEventBus.class);

    private final ApplicationEventPublisher eventPublisher;

    public CrossModuleEventBus(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Async
    public void publishEvent(Object event) {
        logger.info("Publishing cross-module event: {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }

    public void publishEventSynchronously(Object event) {
        logger.info("Publishing cross-module event synchronously: {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }
}