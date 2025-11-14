package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.time.Duration;
import java.time.Instant;

/**
 * Value object representing processing duration in milliseconds.
 * Encapsulates duration logic and provides conversion methods.
 */
public record ProcessingDuration(long milliseconds) {
    
    /**
     * Create a ProcessingDuration with validation.
     */
    public static Result<ProcessingDuration> create(long milliseconds) {
        if (milliseconds < 0) {
            return Result.failure(ErrorDetail.of(
                "INVALID_PROCESSING_DURATION",
                ErrorType.VALIDATION_ERROR,
                "Processing duration cannot be negative, got: " + milliseconds,
                "batch.duration.invalid"
            ));
        }
        return Result.success(new ProcessingDuration(milliseconds));
    }
    
    /**
     * Create ProcessingDuration from two timestamps.
     */
    public static Result<ProcessingDuration> between(Instant start, Instant end) {
        if (start == null || end == null) {
            return Result.failure(ErrorDetail.of(
                "NULL_TIMESTAMP",
                ErrorType.VALIDATION_ERROR,
                "Start and end timestamps cannot be null",
                "batch.duration.null.timestamp"
            ));
        }
        
        if (end.isBefore(start)) {
            return Result.failure(ErrorDetail.of(
                "INVALID_TIMESTAMP_ORDER",
                ErrorType.VALIDATION_ERROR,
                "End timestamp must be after start timestamp",
                "batch.duration.invalid.order"
            ));
        }
        
        long durationMs = Duration.between(start, end).toMillis();
        return create(durationMs);
    }
    
    /**
     * Create ProcessingDuration for ongoing operation (from start to now).
     */
    public static Result<ProcessingDuration> fromStart(Instant start) {
        return between(start, Instant.now());
    }
    
    /**
     * Create a zero duration.
     */
    public static ProcessingDuration zero() {
        return new ProcessingDuration(0);
    }
    
    /**
     * Get duration in seconds.
     */
    public double toSeconds() {
        return milliseconds / 1000.0;
    }
    
    /**
     * Get duration in minutes.
     */
    public double toMinutes() {
        return milliseconds / 60000.0;
    }
    
    /**
     * Get duration in hours.
     */
    public double toHours() {
        return milliseconds / 3600000.0;
    }
    
    /**
     * Convert to Java Duration.
     */
    public Duration toDuration() {
        return Duration.ofMillis(milliseconds);
    }
    
    /**
     * Check if duration is zero.
     */
    public boolean isZero() {
        return milliseconds == 0;
    }
    
    /**
     * Check if duration exceeds a threshold.
     */
    public boolean exceeds(ProcessingDuration threshold) {
        return milliseconds > threshold.milliseconds;
    }
    
    /**
     * Add another duration.
     */
    public ProcessingDuration plus(ProcessingDuration other) {
        return new ProcessingDuration(this.milliseconds + other.milliseconds);
    }
    
    /**
     * Get a human-readable representation.
     */
    public String toHumanReadable() {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.1fs", toSeconds());
        } else if (milliseconds < 3600000) {
            return String.format("%.1fm", toMinutes());
        } else {
            return String.format("%.1fh", toHours());
        }
    }
    
    @Override
    public String toString() {
        return toHumanReadable();
    }
}
