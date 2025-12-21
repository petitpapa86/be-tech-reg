package com.bcbs239.regtech.core.domain.events;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Domain events represent significant business events within a bounded context.
 */
@Getter
@Setter
public abstract class DomainEvent {

    private final String eventId = UUID.randomUUID().toString();
    private String correlationId;
    private Maybe<String> causationId;
    private final Instant timestamp = Instant.now();
    protected String eventType;

    protected DomainEvent(String correlationId) {
        this.correlationId = correlationId;
    }


    public abstract String eventType();
}

