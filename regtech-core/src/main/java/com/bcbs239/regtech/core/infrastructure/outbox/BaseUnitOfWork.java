package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import com.bcbs239.regtech.core.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Base Unit of Work that collects domain events and persists them to outbox.
 * Subclasses can override to add custom logic, but typically just register entities.
 */
public class BaseUnitOfWork {

    private static final Logger logger = LoggerFactory.getLogger(BaseUnitOfWork.class);

    @Autowired
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private EventBus outboxEventBus;

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
     * Save changes: persist collected events to outbox and dispatch internal handlers.
     */
    @Transactional
    public Result<Void> saveChanges() {
        try {
            // Persist domain events to outbox
            if (!domainEvents.isEmpty()) {
                DomainEvent[] eventsArray = domainEvents.toArray(new DomainEvent[0]);
                Result<Void> outboxResult = outboxEventBus.publishAll(eventsArray);
                if (outboxResult.isFailure()) {
                    domainEvents.clear();
                    throw new RuntimeException("Failed to persist events to outbox: " + outboxResult.getError().get().getMessage());
                }

                // Schedule dispatch of internal handlers after commit
                domainEventPublisher.publishEvents(new ArrayList<>(domainEvents));

                domainEvents.clear();
            }

            return Result.success(null);

        } catch (Exception e) {
            domainEvents.clear();
            logger.error("Error in saveChanges", e);
            throw e;
        }
    }
}