package com.bcbs239.regtech.core.shared;

import java.util.UUID;

public class CorrelationId {

    private final String id;

    private CorrelationId(String id) {
        this.id = id;
    }

    public static CorrelationId generate() {
        return new CorrelationId(UUID.randomUUID().toString());
    }

    public static CorrelationId fromString(String id) {
        return new CorrelationId(id);
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}