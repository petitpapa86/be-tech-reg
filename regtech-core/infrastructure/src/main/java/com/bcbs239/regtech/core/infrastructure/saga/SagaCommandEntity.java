package com.bcbs239.regtech.core.infrastructure.saga;

import java.time.Instant;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.bcbs239.regtech.core.domain.saga.SagaCommand;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SagaCommandEntity extends SagaCommand {
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
