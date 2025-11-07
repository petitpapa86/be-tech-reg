package com.bcbs239.regtech.core.domain.events;

import com.bcbs239.regtech.core.domain.shared.Maybe;
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


    protected IntegrationEvent(String correlationId, Maybe<String> causationId, String eventType) {
        super(correlationId, causationId, eventType);
    }
}

