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

    private final UUID id;
    private final LocalDateTime occurredOn;
    private final String eventType;

    protected IntegrationEvent() {
        this.id = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
        this.eventType = this.getClass().getSimpleName();
    }

    @Override
    public String eventType() {
        return getEventType();
    }

    @Override
    public String toString() {
        return String.format("%s{id=%s, occurredOn=%s}", eventType, id, occurredOn);
    }
}

