package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

import java.time.Duration;
import java.time.Instant;

/**
 * Processing timestamps for tracking report generation lifecycle
 * Immutable value object that captures key processing milestones
 */
public record ProcessingTimestamps(
    Instant startedAt, 
    Instant htmlCompletedAt,
    Instant xbrlCompletedAt,
    Instant completedAt,
    Instant failedAt
) {
    
    /**
     * Create timestamps for a newly started process
     */
    public static ProcessingTimestamps started() {
        return new ProcessingTimestamps(Instant.now(), null, null, null, null);
    }
    
    /**
     * Create timestamps with specific start time
     */
    public static ProcessingTimestamps startedAt(Instant startedAt) {
        return new ProcessingTimestamps(startedAt, null, null, null, null);
    }
    
    /**
     * Mark HTML generation as completed
     */
    public ProcessingTimestamps withHtmlCompleted() {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot complete HTML when processing was never started");
        }
        return new ProcessingTimestamps(startedAt, Instant.now(), xbrlCompletedAt, completedAt, failedAt);
    }
    
    /**
     * Mark XBRL generation as completed
     */
    public ProcessingTimestamps withXbrlCompleted() {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot complete XBRL when processing was never started");
        }
        return new ProcessingTimestamps(startedAt, htmlCompletedAt, Instant.now(), completedAt, failedAt);
    }
    
    /**
     * Mark the entire process as completed
     */
    public ProcessingTimestamps withCompleted() {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot complete processing that was never started");
        }
        if (completedAt != null) {
            throw new IllegalStateException("Processing is already completed");
        }
        return new ProcessingTimestamps(startedAt, htmlCompletedAt, xbrlCompletedAt, Instant.now(), failedAt);
    }
    
    /**
     * Mark the process as failed
     */
    public ProcessingTimestamps withFailed() {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot fail processing that was never started");
        }
        return new ProcessingTimestamps(startedAt, htmlCompletedAt, xbrlCompletedAt, completedAt, Instant.now());
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
     * Calculate total generation duration
     * Returns duration from start to completion, or from start to now if still in progress
     */
    public Duration getGenerationDuration() {
        if (startedAt == null) {
            return Duration.ZERO;
        }
        Instant endTime = completedAt != null ? completedAt : 
                         failedAt != null ? failedAt : 
                         Instant.now();
        return Duration.between(startedAt, endTime);
    }
    
    /**
     * Calculate processing duration (alias for getGenerationDuration)
     */
    public Duration getDuration() {
        return getGenerationDuration();
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
    
    /**
     * Check if both HTML and XBRL are completed
     */
    public boolean areBothFormatsCompleted() {
        return htmlCompletedAt != null && xbrlCompletedAt != null;
    }
    
    /**
     * Check if the process has failed
     */
    public boolean isFailed() {
        return failedAt != null;
    }
    
    @Override
    public String toString() {
        if (isFailed()) {
            return String.format("Started: %s, Failed: %s (Duration: %dms)", 
                startedAt, failedAt, getDurationMillis());
        } else if (isCompleted()) {
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
