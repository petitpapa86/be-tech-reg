package com.bcbs239.regtech.iam.application.events;

import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Domain event handler for UserRegisteredEvent.
 * This handler intentionally does not publish cross-module events directly.
 * The `RegisterUserCommandHandler` persists the integration event into the outbox
 * and the outbox processor is responsible for delivering it to other bounded contexts.
 */
@Component("iamUserRegisteredEventHandler")
public class UserRegisteredEventHandler implements DomainEventHandler<UserRegisteredEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final CrossModuleEventBus eventBus;

    public UserRegisteredEventHandler(CrossModuleEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public String eventType() {
        return "UserRegisteredEvent";
    }

    @Override
    public boolean handle(UserRegisteredEvent event) {
        logger.info("Handled UserRegisteredEvent locally for user={} correlation={}", event.getUserId(), event.getCorrelationId());

        // Publish cross-module event directly for billing context to handle
        eventBus.publishEvent(event);
        logger.info("Published UserRegisteredEvent for user={} correlation={}", event.getUserId(), event.getCorrelationId());

        return true;
    }

    @Override
    public Class<UserRegisteredEvent> eventClass() {
        return UserRegisteredEvent.class;
    }
}