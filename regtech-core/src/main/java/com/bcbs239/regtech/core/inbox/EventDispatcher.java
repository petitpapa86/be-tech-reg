package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.application.IntegrationEvent;

public interface EventDispatcher {
    /**
     * Dispatches the event to registered handlers. Returns true if all handlers report success.
     */
    boolean dispatch(IntegrationEvent event, String messageId);
}

