package com.bcbs239.regtech.riskcalculation.domain.analysis;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing the progress of portfolio analysis processing.
 * Tracks total and processed exposures along with timing information.
 */
public record ProcessingProgress(
    int totalExposures,
    int processedExposures,
    Instant startedAt,
    Instant lastUpdateAt
) {
    
    public ProcessingProgress {
        Objects.requireNonNull(startedAt, "Started timestamp cannot be null");
        Objects.requireNonNull(lastUpdateAt, "Last update timestamp cannot be null");
        
        if (totalExposures < 0) {
            throw new IllegalArgumentException("Total exposures cannot be negative");
        }
        if (processedExposures < 0) {
            throw new IllegalArgumentException("Processed exposures cannot be negative");
        }
        if (processedExposures > totalExposures) {
            throw new IllegalArgumentException("Processed exposures cannot exceed total exposures");
        }
        if (lastUpdateAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Last update cannot be before start time");
        }
    }
    
    /**
     * Creates initial progress for a new batch processing session.
     * 
     * @param totalExposures the total number of exposures to process
     * @return initial ProcessingProgress with zero processed exposures
     */
    public static ProcessingProgress initial(int totalExposures) {
        Instant now = Instant.now();
        return new ProcessingProgress(totalExposures, 0, now, now);
    }
    
    /**
     * Creates a new ProcessingProgress with additional processed exposures.
     * 
     * @param additionalCount the number of additional exposures processed
     * @return updated ProcessingProgress with new processed count and timestamp
     */
    public ProcessingProgress addProcessed(int additionalCount) {
        if (additionalCount < 0) {
            throw new IllegalArgumentException("Additional count cannot be negative");
        }
        
        int newProcessedCount = processedExposures + additionalCount;
        if (newProcessedCount > totalExposures) {
            throw new IllegalArgumentException(
                String.format("Adding %d exposures would exceed total: %d + %d > %d",
                    additionalCount, processedExposures, additionalCount, totalExposures));
        }
        
        return new ProcessingProgress(
            totalExposures,
            newProcessedCount,
            startedAt,
            Instant.now()
        );
    }
    
    /**
     * Calculates the percentage of processing completed.
     * 
     * @return percentage complete as a value between 0.0 and 100.0
     */
    public double getPercentageComplete() {
        if (totalExposures == 0) {
            return 100.0; // Consider empty batch as complete
        }
        return (double) processedExposures / totalExposures * 100.0;
    }
    
    /**
     * Estimates the time remaining based on current processing rate.
     * 
     * @return estimated time remaining, or Duration.ZERO if processing is complete or rate cannot be calculated
     */
    public Duration getEstimatedTimeRemaining() {
        if (processedExposures == 0 || processedExposures >= totalExposures) {
            return Duration.ZERO;
        }
        
        Duration elapsed = Duration.between(startedAt, lastUpdateAt);
        if (elapsed.isZero() || elapsed.isNegative()) {
            return Duration.ZERO; // Cannot estimate without elapsed time
        }
        
        // Calculate processing rate (exposures per second)
        double rate = (double) processedExposures / elapsed.toSeconds();
        if (rate <= 0) {
            return Duration.ZERO;
        }
        
        long remainingExposures = totalExposures - processedExposures;
        long estimatedSecondsRemaining = (long) (remainingExposures / rate);
        
        return Duration.ofSeconds(estimatedSecondsRemaining);
    }
    
    /**
     * Gets the current processing rate in exposures per second.
     * 
     * @return processing rate, or 0.0 if rate cannot be calculated
     */
    public double getProcessingRate() {
        if (processedExposures == 0) {
            return 0.0;
        }
        
        Duration elapsed = Duration.between(startedAt, lastUpdateAt);
        if (elapsed.isZero() || elapsed.isNegative()) {
            return 0.0;
        }
        
        return (double) processedExposures / elapsed.toSeconds();
    }
    
    /**
     * Checks if processing is complete.
     * 
     * @return true if all exposures have been processed
     */
    public boolean isComplete() {
        return processedExposures >= totalExposures;
    }
    
    /**
     * Gets the number of remaining exposures to process.
     * 
     * @return remaining exposure count
     */
    public int getRemainingExposures() {
        return totalExposures - processedExposures;
    }
    
    @Override
    public String toString() {
        return String.format("ProcessingProgress{%d/%d (%.1f%%), rate=%.1f/sec, remaining=%s}",
            processedExposures, totalExposures, getPercentageComplete(),
            getProcessingRate(), getEstimatedTimeRemaining());
    }
}
