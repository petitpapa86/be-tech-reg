package com.bcbs239.regtech.core.infrastructure.eventprocessing;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.events.EventBus;
import com.bcbs239.regtech.core.domain.core.Result;
import com.bcbs239.regtech.core.domain.errorhandling.ErrorDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.time.Instant;

/**
 * EventBus implementation that persists domain events to the outbox table.
 */
@Component
public class OutboxEventBus implements EventBus {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventBus.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public OutboxEventBus(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Result<Void> publish(DomainEvent domainEvent) {
        try {
            String content = objectMapper.writeValueAsString(domainEvent);
            String type = domainEvent.getClass().getName();

            OutboxMessageEntity outboxMessage = new OutboxMessageEntity(type, content, Instant.now());
            entityManager.persist(outboxMessage);

            logger.info("Stored domain event in outbox: {} - {}", type, domainEvent);
            return Result.success(null);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize domain event: {}", domainEvent, e);
            return Result.failure(ErrorDetail.of("SERIALIZATION_FAILED",
                "Failed to serialize domain event"));
        }
    }
}
