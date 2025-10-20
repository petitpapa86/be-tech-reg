package com.bcbs239.regtech.core.application;

/**
 * Interface for handling integration events received from other bounded contexts.
 * Implementations should process the events and perform any necessary business logic.
 */
public interface IIntegrationEventHandler<T extends IntegrationEvent> {

    /**
     * Handles the integration event.
     * Implementations should be idempotent as events may be delivered multiple times.
     *
     * @param event The integration event to handle
     */
    void handle(T event);

    /**
     * Returns the class of the event this handler can process.
     *
     * @return The event class
     */
    Class<T> getEventClass();

    /**
     * Returns a unique name for this handler.
     * Used for tracking which handlers have processed which events.
     *
     * @return The handler name
     */
    String getHandlerName();
}