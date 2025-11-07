package com.bcbs239.regtech.core.application.outbox;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simple dispatcher for domain events.
 * Currently just logs the event - can be extended to register handlers.
 */
@Component
public class DomainEventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventDispatcher.class);

    public void dispatch(DomainEvent event) {
        logger.info("Dispatching domain event: {}", event.getClass().getSimpleName());
        // TODO: Implement actual event dispatching to handlers
    }
}