package com.bcbs239.regtech.riskcalculation.domain.analysis;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing metadata about a processed chunk of exposures.
 * Used for progress tracking, performance monitoring, and resumability.
 */
public record ChunkMetadata(
    int index,
    int size,
    Instant processedAt,
    Duration processingTime
) {
    
    public ChunkMetadata {
        Objects.requireNonNull(processedAt, "Processed timestamp cannot be null");
        Objects.requireNonNull(processingTime, "Processing time cannot be null");
        
        if (index < 0) {
            throw new IllegalArgumentException("Chunk index cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (processingTime.isNegative()) {
            throw new IllegalArgumentException("Processing time cannot be negative");
        }
    }
    
    /**
     * Factory method to create ChunkMetadata with current timestamp.
     * 
     * @param index the chunk index (0-based)
     * @param size the number of exposures in the chunk
     * @param processingTime the time taken to process this chunk
     * @return new ChunkMetadata instance
     */
    public static ChunkMetadata of(int index, int size, Duration processingTime) {
        return new ChunkMetadata(index, size, Instant.now(), processingTime);
    }
    
    /**
     * Calculates the processing rate for this chunk in exposures per second.
     * 
     * @return processing rate, or 0.0 if processing time is zero
     */
    public double getProcessingRate() {
        if (processingTime.isZero()) {
            return 0.0;
        }
        return (double) size / processingTime.toSeconds();
    }
    
    /**
     * Gets the processing time in milliseconds.
     * 
     * @return processing time in milliseconds
     */
    public long getProcessingTimeMillis() {
        return processingTime.toMillis();
    }
    
    /**
     * Checks if this chunk was processed recently (within the last minute).
     * 
     * @return true if processed within the last minute
     */
    public boolean isRecentlyProcessed() {
        return Duration.between(processedAt, Instant.now()).compareTo(Duration.ofMinutes(1)) < 0;
    }
    
    @Override
    public String toString() {
        return String.format("ChunkMetadata{index=%d, size=%d, rate=%.1f/sec, time=%dms}",
            index, size, getProcessingRate(), getProcessingTimeMillis());
    }
}
