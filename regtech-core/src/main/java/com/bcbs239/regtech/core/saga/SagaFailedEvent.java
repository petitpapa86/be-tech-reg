package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.function.Supplier;

public class SagaFailedEvent extends SagaMessage {
    private final String sagaType;

    public SagaFailedEvent(SagaId sagaId, String sagaType, Supplier<Instant> timeSupplier) {
        super("SagaFailed", timeSupplier.get(), sagaId);
        this.sagaType = sagaType;
    }

    public String getSagaType() {
        return sagaType;
    }
}