package com.bcbs239.regtech.core.domain.saga;

import com.bcbs239.regtech.core.domain.events.DomainEvent;

import java.time.Instant;

/**
 * Message exchanged between saga participants in different bounded contexts.
 * Supports both commands (requests for action) and events (notifications of state changes).
 */
public abstract class SagaMessage implements DomainEvent {
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

    public SagaId sagaId() {
        return sagaId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }
}

