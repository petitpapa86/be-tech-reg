package com.bcbs239.regtech.core.domain.events;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all integration events that are published across bounded contexts.
 * Integration events represent significant business events that other bounded contexts
 * might be interested in.
 */
@Getter
public abstract class IntegrationEvent implements DomainEvent {

    private final String correlationId;
    private final String eventId;
    private final String aggregateId;
    private final String causationId;
    private final LocalDateTime occurredOn;
    private final String eventType;

    protected IntegrationEvent(String eventId,String eventType, String correlationId, String aggregateId, String causationId) {
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.aggregateId = aggregateId;
        this.causationId = causationId;
        this.eventId = eventId;
        this.occurredOn = LocalDateTime.now();
    }

    @Override
    public String eventType() {
        return getEventType();
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, occurredOn=%s}", eventType, eventId, occurredOn);
    }
}

