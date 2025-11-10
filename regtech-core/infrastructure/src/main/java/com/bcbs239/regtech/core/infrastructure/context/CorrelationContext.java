package com.bcbs239.regtech.core.infrastructure.context;

import java.lang.ScopedValue;

public final class CorrelationContext {

    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> CAUSATION_ID   = ScopedValue.newInstance();
    public static final ScopedValue<String> BOUNDED_CONTEXT = ScopedValue.newInstance();
    public static final ScopedValue<Boolean> OUTBOX_REPLAY = ScopedValue.newInstance();
    public static final ScopedValue<Boolean> INBOX_REPLAY = ScopedValue.newInstance();

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

    public static boolean isOutboxReplay() {
        return OUTBOX_REPLAY.isBound() && Boolean.TRUE.equals(OUTBOX_REPLAY.get());
    }

    public static boolean isInboxReplay() {
        return INBOX_REPLAY.isBound() && Boolean.TRUE.equals(INBOX_REPLAY.get());
    }

    /**
     * Run the given Runnable with the supplied correlation and causation ids
     * scoped via the module-level ScopedValue instances. Either id may be
     * null; if both are null the runnable is executed directly without scoping.
     */
    public static void runWith(String correlationId, String causationId, Runnable r) {
        if (correlationId == null && causationId == null) {
            r.run();
            return;
        }

        var scope = java.lang.ScopedValue.where(CORRELATION_ID, correlationId);
        if (causationId != null) {
            scope = scope.where(CAUSATION_ID, causationId);
        }
        scope.run(r);
    }
}
