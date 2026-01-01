package com.bcbs239.regtech.core.domain.saga;

import java.time.Instant;
import java.util.function.Supplier;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SagaStartedEvent extends SagaMessage {
    private final String sagaType;

    public SagaStartedEvent(SagaId sagaId, String sagaType, Supplier<Instant> timeSupplier) {
        super("SagaStarted", timeSupplier.get(), sagaId, null, null); // correlationId and causationId can be null for saga events
        this.sagaType = sagaType;
    }
}
