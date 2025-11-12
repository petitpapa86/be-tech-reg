package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Getter;

/**
 * Integration event published when event handler invocation fails (no handler found).
 * This event is consumed by the notification module to alert the team.
 */
@Getter
public class EventHandlerInvocationFailed extends IntegrationEvent {

    private final String failureId;
    private final String eventType;
    private final String userId;
    private final int retryCount;
    private final int maxRetries;

    public EventHandlerInvocationFailed(
            String failureId,
            String eventType,
            String userId,
            int retryCount,
            int maxRetries,
            String correlationId) {
        super(correlationId, Maybe.none(), "EventHandlerInvocationFailed");
        this.failureId = failureId;
        this.eventType = eventType;
        this.userId = userId;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
    }

    @Override
    public String eventType() {
        return "EventHandlerInvocationFailed";
    }
}
