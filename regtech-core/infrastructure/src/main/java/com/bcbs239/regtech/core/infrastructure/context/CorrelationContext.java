package com.bcbs239.regtech.core.infrastructure.context;

import java.lang.ScopedValue;

public final class CorrelationContext {

    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> CAUSATION_ID   = ScopedValue.newInstance();
    public static final ScopedValue<String> BOUNDED_CONTEXT = ScopedValue.newInstance();

    private CorrelationContext() {}

    public static String correlationId() {
        return CORRELATION_ID.isBound() ? CORRELATION_ID.get() : null;
    }

    public static String causationId() {
        return CAUSATION_ID.isBound() ? CAUSATION_ID.get() : null;
    }

    public static String boundedContext() {
        return BOUNDED_CONTEXT.isBound() ? BOUNDED_CONTEXT.get() : null;
    }
}