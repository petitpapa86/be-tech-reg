package com.bcbs239.regtech.core.infrastructure.systemservices;

import java.util.UUID;

public record CorrelationId(String id) {

    public static CorrelationId generate() {
        return new CorrelationId(UUID.randomUUID().toString());
    }

    public static CorrelationId fromString(String id) {
        return new CorrelationId(id);
    }
}

