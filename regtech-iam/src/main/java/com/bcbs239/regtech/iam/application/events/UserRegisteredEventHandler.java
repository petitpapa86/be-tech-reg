package com.bcbs239.regtech.iam.application.events;

import com.bcbs239.regtech.core.events.CrossModuleEventBus;
import com.bcbs239.regtech.core.events.DomainEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredEvent;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Domain event handler for UserRegisteredEvent.
 * Publishes the event cross-module to be consumed by billing context.
 */
@Component
public class UserRegisteredEventHandler implements DomainEventHandler<UserRegisteredEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final CrossModuleEventBus crossModuleEventBus;

    public UserRegisteredEventHandler(CrossModuleEventBus crossModuleEventBus) {
        this.crossModuleEventBus = crossModuleEventBus;
    }

    @Override
    public String eventType() {
        return "UserRegisteredEvent";
    }

    @Override
    public boolean handle(UserRegisteredEvent event) {
        try {
            logger.info("Publishing UserRegisteredEvent cross-module for user: {} with bank: {}",
                event.getUserId(), event.getBankId());

            // Create integration event from domain event to maintain separation
            UserRegisteredIntegrationEvent integrationEvent = new UserRegisteredIntegrationEvent(
                this, // source
                event.getUserId(),
                event.getEmail(),
                event.getName(),
                event.getBankId(),
                event.getPaymentMethodId(),
                event.getPhone(),
                event.getAddress(),
                event.getCorrelationId()
            );

            // Publish the integration event cross-module to billing context
            // Using synchronous publishing to ensure reliable delivery
            crossModuleEventBus.publishEventSynchronously(integrationEvent);

            logger.info("Successfully published UserRegisteredIntegrationEvent for user: {} with correlation: {}",
                event.getUserId(), event.getCorrelationId());

            return true;
        } catch (Exception e) {
            logger.error("Failed to publish UserRegisteredIntegrationEvent for user: {}: {}",
                event.getUserId(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Class<UserRegisteredEvent> eventClass() {
        return UserRegisteredEvent.class;
    }
}