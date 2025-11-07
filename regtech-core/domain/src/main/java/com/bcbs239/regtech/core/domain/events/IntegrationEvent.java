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
public abstract class IntegrationEvent extends DomainEvent {

    private final String aggregateId;
    private final LocalDateTime occurredOn;
    private final String eventType;
    private final String sourceContext;
    private final String targetContext;

    protected IntegrationEvent(String eventType, String correlationId, String aggregateId, String causationId, String sourceContext, String targetContext) {
        super(correlationId, causationId);
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.occurredOn = LocalDateTime.now();
        this.sourceContext = sourceContext;
        this.targetContext = targetContext;
    }

    @Override
    public String eventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, occurredOn=%s}", eventType, getEventId(), occurredOn);
    }
}

