package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.shared.Maybe;

import java.time.Instant;
import java.util.Map;

/**
 * Value object representing estimated completion time for batch processing.
 * Encapsulates the logic for calculating when a batch will be completed based on
 * current status, stage durations, and file size.
 */
public record EstimatedCompletionTime(Long epochMillis) {
    
    /**
     * Calculate estimated completion time for a batch in progress.
     * Returns Maybe.none() if the batch is already in a terminal state (COMPLETED or FAILED).
     * 
     * @param currentStatus The current batch status
     * @param stageDurations Map of estimated durations for each processing stage
     * @param fileSize The size of the file being processed
     * @param baseSizeForEstimation The base file size used for time estimation (e.g., 10MB)
     * @return Maybe containing EstimatedCompletionTime, or Maybe.none() if batch is in terminal state
     */
    public static Maybe<EstimatedCompletionTime> calculate(
            BatchStatus currentStatus,
            Map<BatchStatus, ProcessingDuration> stageDurations,
            FileSizeBytes fileSize,
            FileSizeBytes baseSizeForEstimation
    ) {
        // No estimation for terminal states
        if (currentStatus == BatchStatus.COMPLETED || currentStatus == BatchStatus.FAILED) {
            return Maybe.none();
        }
        
        Instant now = Instant.now();
        long remainingMs = 0;
        
        // Add remaining time for all future stages
        for (BatchStatus futureStatus : BatchStatus.values()) {
            if (futureStatus.ordinal() > currentStatus.ordinal()) {
                ProcessingDuration stageDuration = stageDurations.getOrDefault(
                    futureStatus,
                    ProcessingDuration.zero()
                );
                remainingMs += stageDuration.milliseconds();
            }
        }
        
        // Add partial time for current stage based on file size
        if (stageDurations.containsKey(currentStatus)) {
            ProcessingDuration stageEstimate = stageDurations.get(currentStatus);
            
            // Adjust based on file size (larger files take longer)
            double sizeMultiplier = Math.max(1.0, fileSize.multiplierRelativeTo(baseSizeForEstimation));
            long adjustedEstimate = (long) (stageEstimate.milliseconds() * Math.min(sizeMultiplier, 5.0)); // Cap at 5x
            
            // Assume we're halfway through current stage
            remainingMs += adjustedEstimate / 2;
        }
        
        long estimatedEpochMillis = now.toEpochMilli() + remainingMs;
        return Maybe.some(new EstimatedCompletionTime(estimatedEpochMillis));
    }
    
    /**
     * Get the estimated completion time as an Instant.
     */
    public Instant toInstant() {
        return epochMillis != null ? Instant.ofEpochMilli(epochMillis) : null;
    }
    
    /**
     * Get the remaining time in milliseconds from now.
     */
    public long remainingMilliseconds() {
        if (epochMillis == null) {
            return 0;
        }
        long remaining = epochMillis - Instant.now().toEpochMilli();
        return Math.max(0, remaining); // Never return negative
    }
    
    /**
     * Check if the estimated time has passed.
     */
    public boolean isPast() {
        return epochMillis != null && epochMillis < Instant.now().toEpochMilli();
    }
    
    /**
     * Get a human-readable representation of the remaining time.
     */
    public String remainingTimeHumanReadable() {
        long remainingMs = remainingMilliseconds();
        
        if (remainingMs < 1000) {
            return "< 1 second";
        } else if (remainingMs < 60000) {
            return (remainingMs / 1000) + " seconds";
        } else if (remainingMs < 3600000) {
            return (remainingMs / 60000) + " minutes";
        } else {
            return String.format("%.1f hours", remainingMs / 3600000.0);
        }
    }
    
    @Override
    public String toString() {
        return epochMillis != null ? toInstant().toString() : "Not estimated";
    }
}
