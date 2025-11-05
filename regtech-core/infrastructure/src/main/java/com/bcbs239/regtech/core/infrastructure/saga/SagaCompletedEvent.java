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
public class SagaCompletedEvent extends SagaMessage {
    private final String sagaType;

    public SagaCompletedEvent(SagaId sagaId, String sagaType, Supplier<Instant> timeSupplier) {
        super("SagaCompleted", timeSupplier.get(), sagaId);
        this.sagaType = sagaType;
    }

}

