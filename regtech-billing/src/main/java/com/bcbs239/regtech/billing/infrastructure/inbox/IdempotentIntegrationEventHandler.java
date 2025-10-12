package com.bcbs239.regtech.billing.infrastructure.inbox;

import com.bcbs239.regtech.core.events.BaseEvent;

/**
 * Interface for handling integration events with idempotency support.
 * Similar to DomainEventHandler but designed for cross-bounded-context integration events.
 * Implementations should ensure idempotent processing of events.
 */
public interface IdempotentIntegrationEventHandler<T extends BaseEvent> {

    /**
     * The event type this handler processes (e.g., "UserRegisteredIntegrationEvent")
     */
    String eventType();

    /**
     * Handle the integration event.
     * Implementations should be idempotent - processing the same event multiple times
     * should not cause duplicate side effects.
     *
     * @param event The integration event to handle
     * @return true if processing succeeded, false otherwise
     */
    boolean handle(T event);

    /**
     * The event class this handler can process
     */
    Class<T> eventClass();
}