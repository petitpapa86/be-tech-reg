package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Getter;

/**
 * Integration event published when publishing a permanent failure event itself fails.
 * This event is consumed by the notification module to alert the team about critical failures.
 */
@Getter
public class EventPublishingFailed extends IntegrationEvent {

    private final String failureId;
    private final String eventType;
    private final String userId;
    private final String errorMessage;

    public EventPublishingFailed(
            String failureId,
            String eventType,
            String userId,
            String errorMessage,
            String correlationId) {
        super(correlationId, Maybe.none(), "EventPublishingFailed");
        this.failureId = failureId;
        this.eventType = eventType;
        this.userId = userId;
        this.errorMessage = errorMessage;
    }

    @Override
    public String eventType() {
        return "EventPublishingFailed";
    }
}
