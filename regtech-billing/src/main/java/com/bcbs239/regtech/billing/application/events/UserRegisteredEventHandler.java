package com.bcbs239.regtech.billing.application.events;

import com.bcbs239.regtech.core.events.DomainEventHandler;
import com.bcbs239.regtech.core.events.UserRegisteredIntegrationEvent;
import com.bcbs239.regtech.core.inbox.InboxMessageEntity;
import com.bcbs239.regtech.core.inbox.InboxMessageOperations;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Integration event handler for UserRegisteredIntegrationEvent from IAM context.
 * Processes user registration events through the shared inbox infrastructure for reliable delivery.
 * Starts the PaymentVerificationSaga when a user registers.
 */
@Component("billingUserRegisteredIntegrationEventHandler")
@RequiredArgsConstructor
public class UserRegisteredEventHandler implements DomainEventHandler<UserRegisteredIntegrationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(UserRegisteredEventHandler.class);

    private final InboxMessageOperations inboxOperations;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return UserRegisteredIntegrationEvent.class.getSimpleName();
    }

    @Override
    public Class<UserRegisteredIntegrationEvent> eventClass() {
        return UserRegisteredIntegrationEvent.class;
    }

    @Override
    public boolean handle(UserRegisteredIntegrationEvent event) {
        logger.info("Processing UserRegisteredIntegrationEvent for user: {} ({}), email: {}, bankId: {}",
            event.getUserId(),
            event.getName(),
            event.getEmail(),
            event.getBankId());

        try {
            String eventData = objectMapper.writeValueAsString(event);
            String eventType = event.getClass().getSimpleName();
            String aggregateId = event.getUserId();
            InboxMessageEntity entity = new InboxMessageEntity(eventType, eventData, aggregateId);
            inboxOperations.save(entity);
            return true;
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize UserRegisteredIntegrationEvent for user {}: {}",
                event.getUserId(), e.getMessage());
            return false;
        } catch (DataAccessException e) {
            logger.error("Failed to save InboxMessageEntity for user {}: {}",
                event.getUserId(), e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error processing UserRegisteredIntegrationEvent for user {}: {}",
                event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }
}