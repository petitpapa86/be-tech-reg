package com.bcbs239.regtech.core.application.saga;

import com.bcbs239.regtech.core.domain.saga.SagaId;

public class SagaNotFoundException extends RuntimeException {
    public SagaNotFoundException(SagaId sagaId) {
        super("Saga not found with id: " + sagaId.id());
    }
}
