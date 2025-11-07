package com.bcbs239.regtech.core.domain.events;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEvent implements DomainEvent {

    private final String correlationId;
    private final String eventId;
    private final String aggregateId;
    private final String causationId;
    private final LocalDateTime timestamp;

    protected BaseEvent(String correlationId, String eventId, String aggregateId, String causationId) {
        this.correlationId = correlationId;
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.causationId = causationId;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String eventType() {
        // Default event type is the simple class name; subclasses can override if needed
        return this.getClass().getSimpleName();
    }
}

