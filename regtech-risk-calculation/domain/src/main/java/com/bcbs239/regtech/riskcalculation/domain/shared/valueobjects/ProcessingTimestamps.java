package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import java.time.Instant;

/**
 * Processing timestamps for tracking calculation lifecycle
 * Immutable value object that captures key processing milestones
 */
public record ProcessingTimestamps(Instant startedAt, Instant completedAt, Instant failedAt) {
    
    public static ProcessingTimestamps started() {
        return new ProcessingTimestamps(Instant.now(), null, null);
    }
    
    public ProcessingTimestamps completed() {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot complete processing that was never started");
        }
        return new ProcessingTimestamps(startedAt, Instant.now(), failedAt);
    }
    
    public ProcessingTimestamps failed() {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot fail processing that was never started");
        }
        return new ProcessingTimestamps(startedAt, completedAt, Instant.now());
    }
    
    public boolean isStarted() {
        return startedAt != null;
    }
    
    public boolean isCompleted() {
        return completedAt != null;
    }
    
    public boolean isFailed() {
        return failedAt != null;
    }
    
    public boolean isInProgress() {
        return isStarted() && !isCompleted() && !isFailed();
    }
}