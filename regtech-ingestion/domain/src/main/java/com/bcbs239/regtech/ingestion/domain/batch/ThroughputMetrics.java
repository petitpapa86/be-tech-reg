package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Value object representing processing throughput metrics.
 * Encapsulates calculations for records per second and megabytes per second.
 */
public record ThroughputMetrics(
    double recordsPerSecond,
    double megabytesPerSecond
) {
    
    /**
     * Create ThroughputMetrics with validation.
     */
    public static Result<ThroughputMetrics> create(double recordsPerSecond, double megabytesPerSecond) {
        if (recordsPerSecond < 0) {
            return Result.failure(ErrorDetail.of(
                "INVALID_RECORDS_THROUGHPUT",
                ErrorType.VALIDATION_ERROR,
                "Records per second cannot be negative, got: " + recordsPerSecond,
                "batch.throughput.records.invalid"
            ));
        }
        
        if (megabytesPerSecond < 0) {
            return Result.failure(ErrorDetail.of(
                "INVALID_MB_THROUGHPUT",
                ErrorType.VALIDATION_ERROR,
                "Megabytes per second cannot be negative, got: " + megabytesPerSecond,
                "batch.throughput.mb.invalid"
            ));
        }
        
        return Result.success(new ThroughputMetrics(
            Math.round(recordsPerSecond * 100.0) / 100.0,
            Math.round(megabytesPerSecond * 100.0) / 100.0
        ));
    }
    
    /**
     * Calculate throughput from processing metrics.
     */
    public static Result<ThroughputMetrics> calculate(
        int totalRecords,
        FileSizeBytes fileSize,
        ProcessingDuration duration
    ) {
        if (totalRecords < 0) {
            return Result.failure(ErrorDetail.of(
                "INVALID_RECORD_COUNT",
                ErrorType.VALIDATION_ERROR,
                "Total records cannot be negative",
                "batch.throughput.records.negative"
            ));
        }
        
        if (duration.isZero()) {
            return Result.failure(ErrorDetail.of(
                "ZERO_DURATION",
                ErrorType.VALIDATION_ERROR,
                "Cannot calculate throughput with zero duration",
                "batch.throughput.zero.duration"
            ));
        }
        
        double durationSeconds = duration.toSeconds();
        double recordsPerSec = totalRecords / durationSeconds;
        double mbPerSec = fileSize.toMB() / durationSeconds;
        
        return create(recordsPerSec, mbPerSec);
    }
    
    /**
     * Create zero throughput (no processing occurred).
     */
    public static ThroughputMetrics zero() {
        return new ThroughputMetrics(0.0, 0.0);
    }
    
    /**
     * Check if throughput is zero.
     */
    public boolean isZero() {
        return recordsPerSecond == 0.0 && megabytesPerSecond == 0.0;
    }
    
    /**
     * Check if throughput meets a minimum performance threshold.
     */
    public boolean meetsThreshold(ThroughputMetrics threshold) {
        return recordsPerSecond >= threshold.recordsPerSecond &&
               megabytesPerSecond >= threshold.megabytesPerSecond;
    }
    
    /**
     * Compare throughput performance.
     * Returns positive if this throughput is better, negative if worse, 0 if equal.
     */
    public int compareTo(ThroughputMetrics other) {
        // Use weighted average of both metrics
        double thisScore = recordsPerSecond + megabytesPerSecond;
        double otherScore = other.recordsPerSecond + other.megabytesPerSecond;
        return Double.compare(thisScore, otherScore);
    }
    
    /**
     * Get a human-readable representation.
     */
    public String toHumanReadable() {
        return String.format("%.2f records/sec, %.2f MB/sec", recordsPerSecond, megabytesPerSecond);
    }
    
    @Override
    public String toString() {
        return toHumanReadable();
    }
}
