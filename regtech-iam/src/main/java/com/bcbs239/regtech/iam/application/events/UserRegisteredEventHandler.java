package com.bcbs239.regtech.iam.application.events;

import com.bcbs239.regtech.core.application.IIntegrationEventBus;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("iamUserRegisteredEventHandler")
public class UserRegisteredEventHandler implements DomainEventHandler<UserRegisteredEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final IIntegrationEventBus integrationEventBus;

    public UserRegisteredEventHandler(IIntegrationEventBus integrationEventBus) {
        this.integrationEventBus = integrationEventBus;
    }

    @Override
    public boolean handle(UserRegisteredEvent domainEvent) {
        try {
            logger.info("IAM UserRegisteredEventHandler called for user: {}", domainEvent.getUserId());
            logger.info("Converting domain event to integration event for user: {}", domainEvent.getUserId());

            UserRegisteredIntegrationEvent integrationEvent = new UserRegisteredIntegrationEvent(
                domainEvent.getUserId(),
                domainEvent.getEmail(),
                domainEvent.getName(), // Use full name for the core event
                domainEvent.getBankId(),
                domainEvent.getPaymentMethodId(),
                domainEvent.getPhone(),
                domainEvent.getAddress()
            );

            integrationEventBus.publish(integrationEvent);

            logger.info("Published UserRegisteredIntegrationEvent for user: {}", domainEvent.getUserId());
            return true;
        } catch (Exception e) {
            logger.error("Failed to publish integration event for user: {}", domainEvent.getUserId(), e);
            return false;
        }
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