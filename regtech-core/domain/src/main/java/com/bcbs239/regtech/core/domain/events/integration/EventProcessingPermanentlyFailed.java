package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Integration event published when event processing has permanently failed after exhausting all retries.
 * This event is consumed by the notification module to send alerts to the team.
 */
@Getter
public class EventProcessingPermanentlyFailed extends IntegrationEvent {

    private final String failureId;
    private final String eventType;
    private final String userId;
    private final String eventPayload;
    private final int retryCount;

    @JsonCreator
    public EventProcessingPermanentlyFailed(
            @JsonProperty("failureId") String failureId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("userId") String userId,
            @JsonProperty("eventPayload") String eventPayload,
            @JsonProperty("retryCount") int retryCount,
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId, Maybe.none(), "EventProcessingPermanentlyFailed");
        this.failureId = failureId;
        this.eventType = eventType;
        this.userId = userId;
        this.eventPayload = eventPayload;
        this.retryCount = retryCount;
    }

    @Override
    public String eventType() {
        return "EventProcessingPermanentlyFailed";
    }
}