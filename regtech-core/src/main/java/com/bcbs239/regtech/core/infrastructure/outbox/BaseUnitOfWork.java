package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.core.shared.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Unit of Work that collects domain events and persists them to outbox.
 * Subclasses can override to add custom logic, but typically just register entities.
 */
public class BaseUnitOfWork {

    @Autowired
    private EventBus outboxEventBus;

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Collect domain events from an entity.
     */
    protected void collectDomainEvents(com.bcbs239.regtech.core.shared.Entity entity) {
        domainEvents.addAll(entity.getDomainEvents());
        entity.clearDomainEvents();
    }

    /**
     * Collect domain events from multiple entities.
     */
    protected void collectDomainEvents(com.bcbs239.regtech.core.shared.Entity... entities) {
        for (com.bcbs239.regtech.core.shared.Entity entity : entities) {
            collectDomainEvents(entity);
        }
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
    public Result<Void> saveChanges() {
        try {
            // Perform the actual save operations (implemented by subclasses)
            doSaveChanges();

            // Persist domain events to outbox within the same transaction
            if (!domainEvents.isEmpty()) {
                Result<Void> outboxResult = outboxEventBus.publishAll(domainEvents.toArray(DomainEvent[]::new));
                if (outboxResult.isFailure()) {
                    domainEvents.clear();
                    throw new RuntimeException("Failed to persist events to outbox: " + outboxResult.getError().get().getMessage());
                }

//                // Schedule dispatch of internal handlers after commit
//                domainEventPublisher.publishEvents(new ArrayList<>(domainEvents));

                // Clear the collected events now that they've been persisted and scheduled
                domainEvents.clear();
            }

            return Result.success(null);

        } catch (Exception e) {
            domainEvents.clear();
            throw e;
        }
    }

    protected void doSaveChanges() {
        // Default implementation does nothing; subclasses can override
    }
}