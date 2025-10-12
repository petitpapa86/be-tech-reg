package com.bcbs239.regtech.billing.infrastructure.outbox;

import com.bcbs239.regtech.core.events.BaseEvent;

/**
 * Interface for publishing events to the outbox in the billing context.
 * Provides a clean abstraction for storing events that need to be published reliably.
 */
public interface OutboxPublisher {

    /**
     * Publish an event to the outbox for reliable delivery.
     * The event will be stored and published asynchronously by the outbox processor.
     *
     * @param event The event to publish
     * @param correlationId Correlation ID for tracking the event across modules
     */
    void publish(BaseEvent event, String correlationId);

    /**
     * Publish an event to the outbox with a generated correlation ID.
     *
     * @param event The event to publish
     */
    void publish(BaseEvent event);
}