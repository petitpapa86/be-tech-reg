package com.bcbs239.regtech.core.saga;

import java.time.Instant;

import com.bcbs239.regtech.core.events.DomainEvent;
import lombok.Getter;

/**
 * Message exchanged between saga participants in different bounded contexts.
 * Supports both commands (requests for action) and events (notifications of state changes).
 */
@Getter
public abstract  class SagaMessage implements DomainEvent {
    protected final SagaId sagaId;
    protected final Instant occurredAt;
    protected final String eventType;

    public SagaMessage(String eventType, Instant occurredAt, SagaId sagaId) {
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.sagaId = sagaId;
    }

    @Override
    public String eventType() {
        return eventType;
    }
}