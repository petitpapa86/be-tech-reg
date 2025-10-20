package com.bcbs239.regtech.billing.application.events;

import com.bcbs239.regtech.core.application.IIntegrationEventHandler;
import com.bcbs239.regtech.iam.domain.users.events.UserRegisteredIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Integration event handler for UserRegisteredIntegrationEvent from IAM context.
 * Processes user registration events through the shared inbox infrastructure for reliable delivery.
 * Handles billing-specific logic when new users register in the IAM bounded context.
 */
@Component("billingUserRegisteredIntegrationEventHandler")
public class UserRegisteredEventHandler implements IIntegrationEventHandler<UserRegisteredIntegrationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    public UserRegisteredEventHandler() {
        // Constructor can be used for dependency injection if needed in the future
    }

    /**
     * Handle user registered integration event by processing billing logic.
     * Processed asynchronously through the inbox pattern for guaranteed delivery.
     */
    @Override
    public void handle(UserRegisteredIntegrationEvent event) {
        logger.info("Processing UserRegisteredIntegrationEvent for user: {} ({} {}), email: {}, bankId: {}",
            event.getUserId(),
            event.getFirstName(),
            event.getLastName(),
            event.getEmail(),
            event.getBankId());

        try {
            // TODO: Implement billing-specific logic for new user registration:
            // - Create billing account for the user
            // - Set up default billing preferences based on bankId
            // - Initialize subscription if needed
            // - Send welcome billing email
            // - Create audit trail for compliance
            // - Set up payment method collection workflow (separate from registration)

            logger.info("User registration integration event processing completed for billing: userId={}, fullName={} {}",
                event.getUserId(), event.getFirstName(), event.getLastName());

        } catch (Exception e) {
            logger.error("Unexpected error processing UserRegisteredIntegrationEvent for user {}: {}",
                event.getUserId(), e.getMessage(), e);
        }
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