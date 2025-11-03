package com.bcbs239.regtech.core.shared;

import java.util.concurrent.atomic.AtomicReference;

public final class TransitionMetricsHolder {
    private static final AtomicReference<TransitionMetrics> impl = new AtomicReference<>();

    private TransitionMetricsHolder() {}

    public static void set(TransitionMetrics metrics) {
        impl.set(metrics);
    }

    public static TransitionMetrics get() {
        return impl.get();
    }

    public static void clear() {
        impl.set(null);
    }
}

