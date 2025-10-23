package com.bcbs239.regtech.core.saga;

public class SagaNotFoundException extends RuntimeException {
    public SagaNotFoundException(SagaId sagaId) {
        super("Saga not found with id: " + sagaId.id());
    }
}
