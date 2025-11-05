package com.bcbs239.regtech.core.domain.saga;

import java.time.Instant;
import java.util.Map;

public class SagaCommand extends SagaMessage {
    private final Map<String, Object> payload;

    public SagaCommand(SagaId sagaId, String commandType, Map<String, Object> payload, Instant createdAt) {
        super(commandType, createdAt, sagaId);
        this.payload = payload;
    }

    public Map<String, Object> payload() {
        return payload;
    }

    public String commandType() {
        return eventType;
    }

    public Instant createdAt() {
        return occurredAt;
    }
}