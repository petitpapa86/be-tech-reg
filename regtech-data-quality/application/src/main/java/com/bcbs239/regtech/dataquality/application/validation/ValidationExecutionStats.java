package com.bcbs239.regtech.dataquality.application.validation;

import lombok.Getter;

import java.time.Duration;

/**
 * Statistics collector for validation execution.
 * Tracks execution metrics for reporting and monitoring.
 */
@Getter
public class ValidationExecutionStats {

    private int executed;
    private int skipped;
    private int failed;
    private Duration totalTime;

    public ValidationExecutionStats() {
        this.executed = 0;
        this.skipped = 0;
        this.failed = 0;
        this.totalTime = Duration.ZERO;
    }

    public void incrementExecuted() {
        executed++;
    }

    public void incrementSkipped() {
        skipped++;
    }

    public void incrementFailed() {
        failed++;
    }

    public void addExecutionTime(Duration time) {
        totalTime = totalTime.plus(time);
    }

    public void setTotalExecutionTimeMs(long timeMs) {
        totalTime = Duration.ofMillis(timeMs);
    }
}