package com.bcbs239.regtech.core.infrastructure.saga;
public record SagaId(String id) {
    public SagaId {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("SagaId cannot be null or empty");
        }
    }

    public static SagaId generate() {
        return new SagaId(java.util.UUID.randomUUID().toString());
    }

    public static SagaId of(String id) {
        return new SagaId(id);
    }
}
