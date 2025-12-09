package com.bcbs239.regtech.core.application.monitoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory metrics implementation used for tests and lightweight runtime.
 */
public class InMemoryTransitionMetrics implements TransitionMetrics {

    private final Map<String, AtomicInteger> requested = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> success = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failure = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> totalLatencyMs = new ConcurrentHashMap<>();

    private String key(String from, String to) {
        return from + "->" + to;
    }

    @Override
    public void onRequested(String from, String to) {
        requested.computeIfAbsent(key(from, to), k -> new AtomicInteger()).incrementAndGet();
    }

    @Override
    public void onSuccess(String from, String to, long durationMillis) {
        String k = key(from, to);
        success.computeIfAbsent(k, kk -> new AtomicInteger()).incrementAndGet();
        totalLatencyMs.computeIfAbsent(k, kk -> new AtomicLong()).addAndGet(durationMillis);
    }

    @Override
    public void onFailure(String from, String to, long durationMillis) {
        String k = key(from, to);
        failure.computeIfAbsent(k, kk -> new AtomicInteger()).incrementAndGet();
        totalLatencyMs.computeIfAbsent(k, kk -> new AtomicLong()).addAndGet(durationMillis);
    }

    public int getRequested(String from, String to) {
        return requested.getOrDefault(key(from, to), new AtomicInteger(0)).get();
    }

    public int getSuccess(String from, String to) {
        return success.getOrDefault(key(from, to), new AtomicInteger(0)).get();
    }

    public int getFailure(String from, String to) {
        return failure.getOrDefault(key(from, to), new AtomicInteger(0)).get();
    }

    public long getTotalLatencyMs(String from, String to) {
        return totalLatencyMs.getOrDefault(key(from, to), new AtomicLong(0)).get();
    }
}


