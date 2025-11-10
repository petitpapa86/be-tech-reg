package com.bcbs239.regtech.iam.application.integration;

import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.UserRegisteredIntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.iam.domain.users.events.UserRegisteredEvent;
import com.bcbs239.regtech.core.infrastructure.context.CorrelationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("iamUserRegisteredEventPublisher")
public class UserRegisteredEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventPublisher.class);

    private final IIntegrationEventBus eventBus;

    @Autowired
    public UserRegisteredEventPublisher(IIntegrationEventBus eventBus) {
        this.eventBus = eventBus;
    }
    @TransactionalEventListener
    public void handle(UserRegisteredEvent event) {
        if (CorrelationContext.isOutboxReplay()) {
            logger.debug("Skipping integration publish for UserRegisteredEvent {} because this is an outbox replay", event.getEventId());
            return;
        }
        try {
            logger.info("Converting and publishing UserRegisteredIntegrationEvent for user {}", event.getUserId());

            UserRegisteredIntegrationEvent integrationEvent = new UserRegisteredIntegrationEvent(
                    event.getCorrelationId(),
                    event.getCausationId(),
                    event.getUserId(),
                    event.getEmail(),
                    event.getBankId(),
                    event.getPaymentMethodId()
            );

            eventBus.publish(integrationEvent);
            logger.info("Published UserRegisteredIntegrationEvent for user {}", event.getUserId());

        } catch (Exception ex) {
            logger.error("Failed to publish UserRegisteredIntegrationEvent for user {}", event.getUserId(), ex);
            throw ex;
        }
    }
}
