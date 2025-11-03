package com.bcbs239.regtech.core.shared;

/**
 * Lightweight abstraction for tracking transition metrics. Implementations can be Micrometer-backed or simple in-memory for tests.
 */
public interface TransitionMetrics {

    /**
     * Called when a transition from -> to is requested.
     */
    void onRequested(String from, String to);

    /**
     * Called when a transition succeeds. durationMillis may be zero if not measured.
     */
    void onSuccess(String from, String to, long durationMillis);

    /**
     * Called when a transition fails (validation failed). durationMillis may be zero if not measured.
     */
    void onFailure(String from, String to, long durationMillis);
}

