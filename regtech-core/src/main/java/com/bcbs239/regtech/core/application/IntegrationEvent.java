package com.bcbs239.regtech.core.application;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all integration events that are published across bounded contexts.
 * Integration events represent significant business events that other bounded contexts
 * might be interested in.
 */
public abstract class IntegrationEvent {

    private final UUID id;
    private final LocalDateTime occurredOn;
    private final String eventType;

    protected IntegrationEvent() {
        this.id = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public String getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, occurredOn=%s}", eventType, id, occurredOn);
    }
}