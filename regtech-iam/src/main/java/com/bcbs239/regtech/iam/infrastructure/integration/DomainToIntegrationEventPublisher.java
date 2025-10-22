package com.bcbs239.regtech.iam.infrastructure.integration;

import com.bcbs239.regtech.core.application.IIntegrationEventBus;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.bcbs239.regtech.iam.domain.users.events.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DomainToIntegrationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DomainToIntegrationEventPublisher.class);

    private final IIntegrationEventBus integrationEventBus;

    public DomainToIntegrationEventPublisher(IIntegrationEventBus integrationEventBus) {
        this.integrationEventBus = integrationEventBus;
    }

    @TransactionalEventListener
    public void handleUserRegisteredEvent(UserRegisteredEvent domainEvent) {
        logger.info("Converting domain event to integration event for user: {}", domainEvent.getUserId());

        try {
            // Split the full name into first and last name
            String fullName = domainEvent.getName();
            String[] nameParts = fullName != null ? fullName.split(" ", 2) : new String[]{"", ""};
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            UserRegisteredIntegrationEvent integrationEvent = new UserRegisteredIntegrationEvent(
                java.util.UUID.fromString(domainEvent.getUserId()),
                domainEvent.getEmail(),
                firstName,
                lastName,
                domainEvent.getBankId()
            );

            integrationEventBus.publish(integrationEvent);

            logger.info("Published UserRegisteredIntegrationEvent for user: {}", domainEvent.getUserId());
        } catch (Exception e) {
            logger.error("Failed to publish integration event for user: {}", domainEvent.getUserId(), e);
            throw e;
        }
    }
}