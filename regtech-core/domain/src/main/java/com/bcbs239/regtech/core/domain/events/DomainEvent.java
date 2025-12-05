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
    private final String correlationId;
    private final Maybe<String> causationId;
    private final Instant timestamp = Instant.now();
    protected String eventType;

    protected DomainEvent(String correlationId, Maybe<String> causationId, String eventType) {
        this.correlationId = correlationId;
        this.causationId = causationId;
        this.eventType = eventType;
    }

    /**
     * Constructor for domain events that start a correlation chain.
     * CausationId defaults to none since these events are not caused by other events.
     */
    protected DomainEvent(String correlationId, String eventType) {
        this(correlationId, Maybe.none(), eventType);
    }

    public abstract String eventType();
}

