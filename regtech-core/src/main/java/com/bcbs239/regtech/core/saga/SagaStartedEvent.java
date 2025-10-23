package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.function.Supplier;

public class SagaStartedEvent extends SagaMessage {
    private final String sagaType;

    public SagaStartedEvent(SagaId sagaId, String sagaType, Supplier<Instant> timeSupplier) {
        super("SagaStarted", timeSupplier.get(), sagaId);
        this.sagaType = sagaType;
    }

    public String getSagaType() {
        return sagaType;
    }
}