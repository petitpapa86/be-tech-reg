package com.bcbs239.regtech.iam.infrastructure.integration;

import com.bcbs239.regtech.core.application.IIntegrationEventHandler;
import com.bcbs239.regtech.iam.domain.users.events.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("iamUserRegisteredIntegrationEventHandler")
public class UserRegisteredEventHandler implements IIntegrationEventHandler<UserRegisteredIntegrationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    @Override
    public void handle(UserRegisteredIntegrationEvent event) {
        logger.info("Processing user registration integration event for user: {} ({})",
                   event.getUserId(), event.getEmail());

        // TODO: Implement actual business logic such as:
        // - Send welcome email
        // - Create audit log entry
        // - Initialize user preferences
        // - Notify other systems (billing, compliance, etc.)

        logger.info("User registration processing completed for user: {}", event.getUserId());
    }

    @Override
    public Class<UserRegisteredIntegrationEvent> getEventClass() {
        return UserRegisteredIntegrationEvent.class;
    }

    @Override
    public String getHandlerName() {
        return "UserRegisteredEventHandler";
    }
}