package com.bcbs239.regtech.iam.infrastructure.integration;

import com.bcbs239.regtech.core.application.IIntegrationEventBus;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegisteredEventHandler implements DomainEventHandler<UserRegisteredEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final IIntegrationEventBus integrationEventBus;

    public UserRegisteredEventHandler(IIntegrationEventBus integrationEventBus) {
        this.integrationEventBus = integrationEventBus;
    }

    @TransactionalEventListener
    public boolean handle(UserRegisteredEvent domainEvent) {
        logger.info("Converting domain event to integration event for user: {}", domainEvent.getUserId());

        try {
            UserRegisteredIntegrationEvent integrationEvent = new UserRegisteredIntegrationEvent(
                domainEvent.getUserId(),
                domainEvent.getEmail(),
                domainEvent.getName(), // Use full name for the core event
                domainEvent.getBankId(),
                null, // paymentMethodId - not available in domain event
                null, // phone - not available in domain event
                null  // address - not available in domain event
            );

            integrationEventBus.publish(integrationEvent);

            logger.info("Published UserRegisteredIntegrationEvent for user: {}", domainEvent.getUserId());
        } catch (Exception e) {
            logger.error("Failed to publish integration event for user: {}", domainEvent.getUserId(), e);
            throw e;
        }
        return true;
    }

    @Override
    public String eventType() {
        return "UserRegisteredEvent";
    }

    @Override
    public Class<UserRegisteredEvent> eventClass() {
        return UserRegisteredEvent.class;
    }
}