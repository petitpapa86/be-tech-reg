package com.bcbs239.regtech.core.domain.events;

/**
 * Handler contract for typed domain events. Implementations should be Spring components so they
 * can be discovered and registered by the outbox wiring.
 */
public interface DomainEventHandler<T extends BaseEvent> {
    /**
     * Returns the event type string this handler supports (must match OutboxEventEntity.event_type).
     */
    String eventType();

    /**
     * Handle the domain event. Return true on success, false to indicate failure/retry.
     */
    void handle(T event);

    /**
     * Returns the event class this handler expects for typed deserialization.
     */
    Class<T> eventClass();
}

