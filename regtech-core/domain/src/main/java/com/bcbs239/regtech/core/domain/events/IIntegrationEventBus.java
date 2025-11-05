package com.bcbs239.regtech.core.domain.events;

/**
 * Interface for publishing integration events across bounded contexts.
 * Implementations should ensure reliable delivery of events.
 */
public interface IIntegrationEventBus {

    /**
     * Publishes an integration event to all interested bounded contexts.
     * The implementation should ensure the event is reliably delivered.
     *
     * @param event The integration event to publish
     */
    void publish(IntegrationEvent event);
}
