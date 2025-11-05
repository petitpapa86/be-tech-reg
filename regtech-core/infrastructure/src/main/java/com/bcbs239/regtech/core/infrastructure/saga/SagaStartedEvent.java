package com.bcbs239.regtech.core.infrastructure.saga;

import com.bcbs239.regtech.core.domain.saga.SagaId;
import com.bcbs239.regtech.core.domain.saga.SagaMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.function.Supplier;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SagaStartedEvent extends SagaMessage {
    private final String sagaType;

    public SagaStartedEvent(SagaId sagaId, String sagaType, Supplier<Instant> timeSupplier) {
        super("SagaStarted", timeSupplier.get(), sagaId);
        this.sagaType = sagaType;
    }

}
