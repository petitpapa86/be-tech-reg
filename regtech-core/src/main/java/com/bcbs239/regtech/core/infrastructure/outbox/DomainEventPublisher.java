package com.bcbs239.regtech.core.infrastructure.outbox;

import com.bcbs239.regtech.core.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Publisher for domain events that dispatches after transaction commit.
 */
@Component
public class DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final DomainEventDispatcher eventDispatcher;

    public DomainEventPublisher(DomainEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    /**
     * Publish events after transaction commit if in transaction, otherwise immediately.
     */
    public void publishEvents(List<DomainEvent> domainEvents) {
        if (domainEvents.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Register synchronization to dispatch after commit
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishEventsAfterCommit(domainEvents);
                }
            });
        } else {
            // Dispatch immediately if not in transaction
            publishEventsAfterCommit(domainEvents);
        }
    }

    private void publishEventsAfterCommit(List<DomainEvent> domainEvents) {
        try {
            logger.info("Dispatching {} domain events after transaction commit", domainEvents.size());

            for (DomainEvent domainEvent : domainEvents) {
                eventDispatcher.dispatch(domainEvent);
            }
        } catch (Exception e) {
            logger.error("Failed to dispatch domain events after transaction commit", e);
            // In production, implement retry or dead letter queue
        }
    }
}