package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonCreator
    public EventDeserializationFailed(
            @JsonProperty("failureId") String failureId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("userId") String userId,
            @JsonProperty("retryCount") int retryCount,
            @JsonProperty("maxRetries") int maxRetries,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId);
        this.failureId = failureId;
        this.eventType = eventType;
        this.userId = userId;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.errorMessage = errorMessage;
    }
}
