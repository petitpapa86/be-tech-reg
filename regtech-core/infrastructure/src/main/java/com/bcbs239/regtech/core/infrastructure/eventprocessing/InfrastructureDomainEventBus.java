package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.DomainEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Infrastructure implementation of DomainEventBus.
 * Publishes domain events for processing by registered handlers.
 */
@Component
public class InfrastructureDomainEventBus implements DomainEventBus {

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureDomainEventBus.class);

    private final ApplicationEventPublisher delegate;

    public InfrastructureDomainEventBus(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean publish(DomainEvent event) {
        logger.info("Publishing domain event: {}", event.getClass().getSimpleName());
        delegate.publishEvent(event);
        return true;
    }
}