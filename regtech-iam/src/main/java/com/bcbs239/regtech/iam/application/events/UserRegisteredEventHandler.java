package com.bcbs239.regtech.iam.application.events;

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

    public UserRegisteredEventHandler() {
    }

    @Override
    public String eventType() {
        return "UserRegisteredEvent";
    }

    @Override
    public boolean handle(UserRegisteredEvent event) {
        // This handler intentionally does not publish the integration event.
        // The outbox write is performed by the command handler; keep this handler lightweight.
        logger.info("Handled UserRegisteredEvent locally for user={} correlation={}", event.getUserId(), event.getCorrelationId());
        return true;
    }

    @Override
    public Class<UserRegisteredEvent> eventClass() {
        return UserRegisteredEvent.class;
    }
}