package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import java.time.Duration;
import java.time.Instant;

/**
 * Processing timestamps for tracking report generation lifecycle
 * Immutable value object that captures key processing milestones
 */
public record ProcessingTimestamps(Instant startedAt, Instant completedAt) {
    
    /**
     * Create timestamps for a newly started process
     */
    public static ProcessingTimestamps start() {
        return new ProcessingTimestamps(Instant.now(), null);
    }
    
    /**
     * Create timestamps with specific start time
     */
    public static ProcessingTimestamps startedAt(Instant startedAt) {
        return new ProcessingTimestamps(startedAt, null);
    }
    
    /**
     * Mark the process as completed
     */
    public ProcessingTimestamps complete() {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot complete processing that was never started");
        }
        if (completedAt != null) {
            throw new IllegalStateException("Processing is already completed");
        }
        return new ProcessingTimestamps(startedAt, Instant.now());
    }
    
    /**
     * Mark the process as completed at a specific time
     */
    public ProcessingTimestamps completeAt(Instant completedAt) {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot complete processing that was never started");
        }
        if (this.completedAt != null) {
            throw new IllegalStateException("Processing is already completed");
        }
        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Completion time cannot be before start time");
        }
        return new ProcessingTimestamps(startedAt, completedAt);
    }
    
    /**
     * Check if processing has started
     */
    public boolean isStarted() {
        return startedAt != null;
    }
    
    /**
     * Check if processing has completed
     */
    public boolean isCompleted() {
        return completedAt != null;
    }
    
    /**
     * Check if processing is in progress
     */
    public boolean isInProgress() {
        return isStarted() && !isCompleted();
    }
    
    /**
     * Calculate processing duration
     * Returns duration from start to completion, or from start to now if still in progress
     */
    public Duration getDuration() {
        if (startedAt == null) {
            return Duration.ZERO;
        }
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, endTime);
    }
    
    /**
     * Get duration in milliseconds
     */
    public long getDurationMillis() {
        return getDuration().toMillis();
    }
    
    /**
     * Get duration in seconds
     */
    public long getDurationSeconds() {
        return getDuration().getSeconds();
    }
    
    @Override
    public String toString() {
        if (isCompleted()) {
            return String.format("Started: %s, Completed: %s (Duration: %dms)", 
                startedAt, completedAt, getDurationMillis());
        } else if (isStarted()) {
            return String.format("Started: %s, In Progress (Duration: %dms)", 
                startedAt, getDurationMillis());
        } else {
            return "Not started";
        }
    }
}
