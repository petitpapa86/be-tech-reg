package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Integration event published when event handler execution fails with an exception.
 * This event is consumed by the notification module to alert the team.
 */
@Getter
public class EventHandlerExecutionFailed extends IntegrationEvent {

    private final String failureId;
    private final String eventType;
    private final String userId;
    private final String handlerClass;
    private final String handlerMethod;
    private final String errorMessage;
    private final int retryCount;
    private final int maxRetries;

    @JsonCreator
    public EventHandlerExecutionFailed(
            @JsonProperty("failureId") String failureId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("userId") String userId,
            @JsonProperty("handlerClass") String handlerClass,
            @JsonProperty("handlerMethod") String handlerMethod,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("retryCount") int retryCount,
            @JsonProperty("maxRetries") int maxRetries,
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId, Maybe.none(), "EventHandlerExecutionFailed");
        this.failureId = failureId;
        this.eventType = eventType;
        this.userId = userId;
        this.handlerClass = handlerClass;
        this.handlerMethod = handlerMethod;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
    }

    @Override
    public String eventType() {
        return "EventHandlerExecutionFailed";
    }
}
