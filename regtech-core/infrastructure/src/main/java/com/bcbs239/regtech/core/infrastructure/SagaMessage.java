package com.bcbs239.regtech.core.infrastructure;

import com.bcbs239.regtech.core.domain.DomainEvent;
import com.bcbs239.regtech.core.infrastructure.saga.SagaId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Message exchanged between saga participants in different bounded contexts.
 * Supports both commands (requests for action) and events (notifications of state changes).
 */
@Getter
@ToString
@EqualsAndHashCode
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
