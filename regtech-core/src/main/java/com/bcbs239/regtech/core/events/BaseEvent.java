package com.bcbs239.regtech.core.events;

import java.time.LocalDateTime;

public abstract class BaseEvent implements DomainEvent {

    private final String correlationId;
    private final LocalDateTime timestamp;
    private final String sourceModule;

    protected BaseEvent(String correlationId, String sourceModule) {
        this.correlationId = correlationId;
        this.timestamp = LocalDateTime.now();
        this.sourceModule = sourceModule;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    @Override
    public String eventType() {
        // Default event type is the simple class name; subclasses can override if needed
        return this.getClass().getSimpleName();
    }
}