package com.bcbs239.regtech.core.infrastructure.saga;

import java.time.Instant;
import java.util.function.Supplier;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SagaCompletedEvent extends SagaMessage {
    private final String sagaType;

    public SagaCompletedEvent(SagaId sagaId, String sagaType, Supplier<Instant> timeSupplier) {
        super("SagaCompleted", timeSupplier.get(), sagaId);
        this.sagaType = sagaType;
    }

    public String getSagaType() {
        return sagaType;
    }
}
