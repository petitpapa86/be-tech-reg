package com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects;

import java.time.Instant;
import java.util.Optional;

/**
 * Processing timestamps for tracking calculation lifecycle
 * Immutable value object that captures key processing milestones
 */
public record ProcessingTimestamps(Instant startedAt, Instant completedAt, Instant failedAt) {
    
    public static ProcessingTimestamps started(Instant startedAt) {
        return new ProcessingTimestamps(startedAt, null, null);
    }
    
    public static ProcessingTimestamps started() {
        return started(Instant.now());
    }
    
    public ProcessingTimestamps withCompleted(Instant completedAt) {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot complete processing that was never started");
        }
        return new ProcessingTimestamps(startedAt, completedAt, failedAt);
    }
    
    public ProcessingTimestamps completed() {
        return withCompleted(Instant.now());
    }
    
    public ProcessingTimestamps withFailed(Instant failedAt) {
        if (startedAt == null) {
            throw new IllegalStateException("Cannot fail processing that was never started");
        }
        return new ProcessingTimestamps(startedAt, completedAt, failedAt);
    }
    
    public ProcessingTimestamps failed() {
        return withFailed(Instant.now());
    }
    
    /**
     * Reconstitute from persistence
     */
    public static ProcessingTimestamps reconstitute(
            Instant startedAt,
            Optional<Instant> completedAt,
            Optional<Instant> failedAt) {
        return new ProcessingTimestamps(
            startedAt,
            completedAt.orElse(null),
            failedAt.orElse(null)
        );
    }
    
    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }
    
    public Optional<Instant> getFailedAt() {
        return Optional.ofNullable(failedAt);
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