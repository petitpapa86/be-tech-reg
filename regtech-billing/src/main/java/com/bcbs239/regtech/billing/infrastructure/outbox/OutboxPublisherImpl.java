package com.bcbs239.regtech.billing.infrastructure.outbox;

import com.bcbs239.regtech.core.events.BaseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of OutboxPublisher for the billing context.
 * Stores events in the outbox table for reliable asynchronous publishing.
 */
@Component
public class OutboxPublisherImpl implements OutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxPublisherImpl.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public OutboxPublisherImpl(
            OutboxMessageRepository outboxMessageRepository,
            ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(BaseEvent event, String correlationId) {
        try {
            String eventType = event.getClass().getSimpleName();
            String payload = objectMapper.writeValueAsString(event);

            OutboxMessage outboxMessage = new OutboxMessage(eventType, payload, correlationId);
            outboxMessageRepository.save(outboxMessage);

            logger.info("📤 Published event to outbox: type={}, correlationId={}, id={}",
                eventType, correlationId, outboxMessage.getId());

        } catch (Exception e) {
            logger.error("Failed to publish event to outbox: {} with correlationId: {}",
                event.getClass().getSimpleName(), correlationId, e);
            throw new RuntimeException("Failed to publish event to outbox", e);
        }
    }

    @Override
    @Transactional
    public void publish(BaseEvent event) {
        String correlationId = UUID.randomUUID().toString();
        publish(event, correlationId);
    }
}