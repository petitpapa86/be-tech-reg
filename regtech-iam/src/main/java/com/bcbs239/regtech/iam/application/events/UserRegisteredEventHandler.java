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

        try {
            // Publish cross-module event asynchronously using virtual threads
            eventBus.publishEvent(event);
            logger.info("Initiated async publishing for UserRegisteredEvent user={} correlation={}", event.getUserId(), event.getCorrelationId());

            // Return true immediately - async operation initiated successfully
            // Errors are handled within the virtual thread with proper logging
            return true;

        } catch (Exception e) {
            // This would be very rare - only if async initiation itself fails
            logger.error("❌ Failed to initiate async publishing for UserRegisteredEvent user={} correlation={}: {}",
                event.getUserId(), event.getCorrelationId(), e.getMessage(), e);
            return false; // Return false only for initiation failures
        }
    }

    @Override
    public Class<UserRegisteredEvent> eventClass() {
        return UserRegisteredEvent.class;
    }
}