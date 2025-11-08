package com.bcbs239.regtech.core.application;

import com.bcbs239.regtech.core.domain.outbox.IOutboxMessageRepository;
import com.bcbs239.regtech.core.domain.outbox.OutboxMessage;
import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.infrastructure.eventprocessing.OutboxMessageEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Base Unit of Work that collects domain events and persists them to outbox.
 * Subclasses can override to add custom logic, but typically just register entities.
 */
@Component
@Scope("prototype") 
@RequiredArgsConstructor
public class BaseUnitOfWork {

    private static final Logger logger = LoggerFactory.getLogger(BaseUnitOfWork.class);
    private final IOutboxMessageRepository outboxMessageRepository;

    private final ObjectMapper objectMapper;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Collect domain events from an entity.
     */
    protected void collectDomainEvents(Entity entity) {
        domainEvents.addAll(entity.getDomainEvents());
        entity.clearDomainEvents();
    }

    /**
     * Register an entity whose domain events should be collected.
     */
    public void registerEntity(Entity entity) {
        collectDomainEvents(entity);
    }

    /**
     * Save changes: persist collected events to outbox and schedule internal dispatch after commit.
     * Events will be dispatched by the OutboxProcessor background job for external systems.
     */
    @Transactional
    public void saveChanges() {
        if (!domainEvents.isEmpty()) {
            List<OutboxMessage> outboxMessages = new ArrayList<>();
            for (DomainEvent domainEvent : domainEvents) {
                String content;
                try {
                    content = objectMapper.writeValueAsString(domainEvent);
                } catch (JsonProcessingException e) {
                    logger.error("Failed to serialize domain event: {}", domainEvent, e);
                    throw new IllegalStateException("Failed to serialize domain events for outbox persistence.", e);
                }
                String type = domainEvent.getClass().getName();
                OutboxMessage outboxMessage = new OutboxMessage(type, content, Instant.now());
                outboxMessages.add(outboxMessage);
            }
            // Persist all messages in a single batch within the same transaction.
            // Use saveAll when available to reduce DB round-trips and ensure
            // persistence happens together with the calling transaction.
            try {
                outboxMessageRepository.saveAll(outboxMessages);
            } catch (Exception e) {
                logger.error("Failed to persist outbox messages", e);
                throw e;
            }
            // Clear only after successful persistence
            domainEvents.clear();
        }
    }
}

