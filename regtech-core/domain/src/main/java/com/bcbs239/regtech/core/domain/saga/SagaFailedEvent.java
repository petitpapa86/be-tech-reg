package com.bcbs239.regtech.core.domain.saga;

import java.time.Instant;
import java.util.function.Supplier;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SagaFailedEvent extends SagaMessage {
    private final String sagaType;

    public SagaFailedEvent(SagaId sagaId, String sagaType, Supplier<Instant> timeSupplier) {
        super("SagaFailed", timeSupplier.get(), sagaId);
        this.sagaType = sagaType;
    }

}
