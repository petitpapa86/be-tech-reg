package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Getter;

/**
 * Integration event published when event deserialization fails during retry processing.
 * This event is consumed by the notification module to alert the team.
 */
@Getter
public class EventDeserializationFailed extends IntegrationEvent {

    private final String failureId;
    private final String eventType;
    private final String userId;
    private final int retryCount;
    private final int maxRetries;
    private final String errorMessage;

    public EventDeserializationFailed(
            String failureId,
            String eventType,
            String userId,
            int retryCount,
            int maxRetries,
            String errorMessage,
            String correlationId) {
        super(correlationId, Maybe.none(), "EventDeserializationFailed");
        this.failureId = failureId;
        this.eventType = eventType;
        this.userId = userId;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.errorMessage = errorMessage;
    }

    @Override
    public String eventType() {
        return "EventDeserializationFailed";
    }
}
