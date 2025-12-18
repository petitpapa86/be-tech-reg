package com.bcbs239.regtech.dataquality.application.validation;

import lombok.Getter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Statistics collector for validation execution.
 * Tracks execution metrics for reporting and monitoring.
 */
@Getter
public class ValidationExecutionStats {

    private final AtomicInteger executed = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);
    private final LongAdder totalExecutionTimeMs = new LongAdder();

    public void incrementExecuted() {
        executed.incrementAndGet();
    }

    public void incrementExecuted(long executionTimeMs) {
        executed.incrementAndGet();
        totalExecutionTimeMs.add(executionTimeMs);
    }

    public void incrementFailed() {
        failed.incrementAndGet();
    }

    public void incrementFailed(long executionTimeMs) {
        failed.incrementAndGet();
        totalExecutionTimeMs.add(executionTimeMs);
    }

    public void incrementSkipped() {
        skipped.incrementAndGet();
    }

    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs.sum();
    }

    public long getAverageExecutionTimeMs() {
        int count = executed.get();
        return count > 0 ? totalExecutionTimeMs.sum() / count : 0;
    }
}