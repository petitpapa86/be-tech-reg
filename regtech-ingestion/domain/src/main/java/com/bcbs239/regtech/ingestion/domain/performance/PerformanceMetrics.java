package com.bcbs239.regtech.ingestion.domain.performance;

import java.time.Instant;

/**
 * Domain model representing performance metrics for ingestion operations.
 */
public record PerformanceMetrics(
    String operationType,
    long durationMs,
    long fileSizeBytes,
    int recordCount,
    boolean successful,
    String errorType,
    Instant timestamp
) {
    
    public PerformanceMetrics {
        if (operationType == null || operationType.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation type cannot be null or empty");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        if (fileSizeBytes < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
        if (recordCount < 0) {
            throw new IllegalArgumentException("Record count cannot be negative");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }
    
    /**
     * Calculate processing rate in records per second.
     */
    public double getRecordsPerSecond() {
        if (durationMs == 0) return 0.0;
        return (double) recordCount / (durationMs / 1000.0);
    }
    
    /**
     * Calculate throughput in MB per second.
     */
    public double getThroughputMBps() {
        if (durationMs == 0) return 0.0;
        double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
        return fileSizeMB / (durationMs / 1000.0);
    }
    
    /**
     * Check if performance is within acceptable thresholds.
     */
    public boolean isPerformanceAcceptable(long maxDurationMs, double minThroughputMBps) {
        return successful && 
               durationMs <= maxDurationMs && 
               getThroughputMBps() >= minThroughputMBps;
    }
}