package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * EventBus implementation that persists domain events to the outbox table.
 */
@Component
public class OutboxEventBus implements EventBus {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventBus.class);

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventBus(OutboxMessageRepository outboxMessageRepository, ObjectMapper objectMapper) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Result<Void> publish(DomainEvent domainEvent) {
        try {
            String content = objectMapper.writeValueAsString(domainEvent);
            String type = domainEvent.getClass().getName();

            OutboxMessageEntity outboxMessage = new OutboxMessageEntity(type, content, Instant.now());
            outboxMessageRepository.save(outboxMessage);

            logger.info("Stored domain event in outbox: {} - {}", type, domainEvent);
            return Result.success(null);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize domain event: {}", domainEvent, e);
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of("SERIALIZATION_FAILED",
                "Failed to serialize domain event", "error.serialization"));
        }
    }
}