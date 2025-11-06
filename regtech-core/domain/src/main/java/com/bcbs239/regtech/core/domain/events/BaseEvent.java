package com.bcbs239.regtech.core.domain.events;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEvent implements DomainEvent {

    private String correlationId;
    private final LocalDateTime timestamp;

    protected BaseEvent() {
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String eventType() {
        // Default event type is the simple class name; subclasses can override if needed
        return this.getClass().getSimpleName();
    }
}

