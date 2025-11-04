package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.core.shared.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequiredArgsConstructor
public class BaseUnitOfWork {

    private static final Logger logger = LoggerFactory.getLogger(BaseUnitOfWork.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Collect domain events from an entity.
     */
    protected void collectDomainEvents(com.bcbs239.regtech.core.shared.Entity entity) {
        domainEvents.addAll(entity.getDomainEvents());
        entity.clearDomainEvents();
    }


    /**
     * Register an entity whose domain events should be collected.
     */
    public void registerEntity(com.bcbs239.regtech.core.shared.Entity entity) {
        collectDomainEvents(entity);
    }

    /**
     * Save changes: persist collected events to outbox and schedule internal dispatch after commit.
     * Events will be dispatched by the OutboxProcessor background job for external systems.
     */
    @Transactional
    public void saveChanges() {

        if (!domainEvents.isEmpty()) {
            for (DomainEvent domainEvent : domainEvents) {
                String content = null;
                try {
                    content = objectMapper.writeValueAsString(domainEvent);
                } catch (JsonProcessingException e) {
                    domainEvents.clear();
                }
                String type = domainEvent.getClass().getName();

                OutboxMessageEntity outboxMessage = new OutboxMessageEntity(type, content, Instant.now());
                entityManager.persist(outboxMessage);
            }
            if (domainEvents.isEmpty()) {
                throw new IllegalStateException("Failed to serialize domain events for outbox persistence.");
            }
        }
        domainEvents.clear();

    }

}