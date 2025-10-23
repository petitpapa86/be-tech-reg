package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.function.Supplier;

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