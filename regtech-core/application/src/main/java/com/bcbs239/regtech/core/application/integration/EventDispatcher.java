package com.bcbs239.regtech.core.application.integration;


import com.bcbs239.regtech.core.domain.events.IntegrationEvent;

/**
 * Interface for dispatching integration events to registered handlers.
 */
public interface EventDispatcher {

    /**
     * Dispatches an integration event to all registered handlers.
     *
     * @param event the integration event to dispatch
     * @param messageId the ID of the message being processed (for logging/tracking)
     * @return true if all handlers succeeded, false if any handler failed
     */
    boolean dispatch(IntegrationEvent event, String messageId);
}
