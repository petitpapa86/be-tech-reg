package com.bcbs239.regtech.reportgeneration.application.coordination;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application component that tracks arrival of dual events (calculation and quality)
 * for each batch. Uses thread-safe ConcurrentHashMap to handle concurrent event
 * arrival. Automatically cleans up expired events older than 24 hours.
 * 
 * This component implements the coordination logic required by Requirements 1.4, 1.5, 2.1.
 * 
 * Design Note: Uses internal event DTOs to avoid coupling to other modules' application layers.
 */
@Component
@Slf4j
public class BatchEventTracker {
    
    private static final Duration EVENT_EXPIRATION = Duration.ofHours(24);
    private static final long CLEANUP_INTERVAL_MINUTES = 30;
    
    private final ConcurrentHashMap<String, BatchEvents> tracker = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor;
    
    public BatchEventTracker() {
        this.cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread thread = new Thread(r, "batch-event-tracker-cleanup");
            thread.setDaemon(true);
            return thread;
        });
        
        // Schedule cleanup every 30 minutes
        this.cleanupExecutor.scheduleAtFixedRate(
            this::cleanupExpiredEvents, 
            CLEANUP_INTERVAL_MINUTES, 
            CLEANUP_INTERVAL_MINUTES, 
            TimeUnit.MINUTES
        );
        
        log.info("BatchEventTracker initialized with cleanup interval: {} minutes", CLEANUP_INTERVAL_MINUTES);
    }
    
    /**
     * Marks risk calculation as complete for a batch.
     * Thread-safe operation using ConcurrentHashMap.computeIfAbsent.
     * 
     * @param batchId the batch identifier
     * @param eventData the calculation event data
     */
    public void markRiskComplete(String batchId, CalculationEventData eventData) {
        log.debug("Marking risk calculation complete for batch: {}", batchId);
        
        BatchEvents events = tracker.computeIfAbsent(batchId, k -> new BatchEvents());
        events.setRiskEventData(eventData);
        events.setRiskComplete(true);
        
        if (events.getFirstEventTime() == null) {
            events.setFirstEventTime(Instant.now());
        }
        
        log.info("Risk calculation marked complete for batch: {}. Quality complete: {}", 
            batchId, events.isQualityComplete());
    }
    
    /**
     * Marks quality validation as complete for a batch.
     * Thread-safe operation using ConcurrentHashMap.computeIfAbsent.
     * 
     * @param batchId the batch identifier
     * @param eventData the quality event data
     */
    public void markQualityComplete(String batchId, QualityEventData eventData) {
        log.debug("Marking quality validation complete for batch: {}", batchId);
        
        BatchEvents events = tracker.computeIfAbsent(batchId, k -> new BatchEvents());
        events.setQualityEventData(eventData);
        events.setQualityComplete(true);
        
        if (events.getFirstEventTime() == null) {
            events.setFirstEventTime(Instant.now());
        }
        
        log.info("Quality validation marked complete for batch: {}. Risk complete: {}", 
            batchId, events.isRiskComplete());
    }
    
    /**
     * Checks if both events (calculation and quality) have arrived for a batch.
     * 
     * @param batchId the batch identifier
     * @return true if both events are present, false otherwise
     */
    public boolean areBothComplete(String batchId) {
        BatchEvents events = tracker.get(batchId);
        boolean bothComplete = events != null && events.isRiskComplete() && events.isQualityComplete();
        
        log.debug("Checking if both events complete for batch: {}. Result: {}", batchId, bothComplete);
        
        return bothComplete;
    }
    
    /**
     * Retrieves both events for a batch.
     * Should only be called after areBothComplete returns true.
     * 
     * @param batchId the batch identifier
     * @return BatchEvents containing both events
     * @throws IllegalStateException if both events are not present
     */
    public BatchEvents getBothEvents(String batchId) {
        BatchEvents events = tracker.get(batchId);
        
        if (events == null || !events.isRiskComplete() || !events.isQualityComplete()) {
            throw new IllegalStateException(
                String.format("Cannot retrieve events for batch %s: both events not present", batchId)
            );
        }
        
        log.debug("Retrieved both events for batch: {}", batchId);
        
        return events;
    }
    
    /**
     * Removes tracking data for a batch after report generation completes.
     * This prevents memory leaks and allows the batch to be reprocessed if needed.
     * 
     * @param batchId the batch identifier
     */
    public void cleanup(String batchId) {
        BatchEvents removed = tracker.remove(batchId);
        
        if (removed != null) {
            log.info("Cleaned up event tracking for batch: {}", batchId);
        } else {
            log.warn("Attempted to cleanup non-existent batch: {}", batchId);
        }
    }
    
    /**
     * Removes expired events older than 24 hours.
     * Runs automatically every 30 minutes via scheduled executor.
     * Implements requirement 1.6 for stale event handling.
     */
    void cleanupExpiredEvents() {
        Instant cutoff = Instant.now().minus(EVENT_EXPIRATION);
        int removedCount = 0;
        
        log.debug("Starting cleanup of expired events. Cutoff time: {}", cutoff);
        
        for (var entry : tracker.entrySet()) {
            String batchId = entry.getKey();
            BatchEvents events = entry.getValue();
            
            if (events.getFirstEventTime() != null && events.getFirstEventTime().isBefore(cutoff)) {
                tracker.remove(batchId);
                removedCount++;
                log.warn("Removed expired event tracking for batch: {}. First event time: {}", 
                    batchId, events.getFirstEventTime());
            }
        }
        
        if (removedCount > 0) {
            log.info("Cleanup completed. Removed {} expired event(s). Remaining: {}", 
                removedCount, tracker.size());
        } else {
            log.debug("Cleanup completed. No expired events found. Remaining: {}", tracker.size());
        }
    }
    
    /**
     * Returns the current number of batches being tracked.
     * Useful for monitoring and health checks.
     * 
     * @return number of batches in tracker
     */
    public int getTrackedBatchCount() {
        return tracker.size();
    }
    
    /**
     * Checks if a specific batch is being tracked.
     * 
     * @param batchId the batch identifier
     * @return true if batch is being tracked, false otherwise
     */
    public boolean isTracking(String batchId) {
        return tracker.containsKey(batchId);
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down BatchEventTracker cleanup executor");
        cleanupExecutor.shutdown();
        
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Inner class representing the pair of event data for a batch.
     * Tracks both calculation and quality event data along with completion status.
     */
    @Getter
    @Setter
    public static class BatchEvents {
        private CalculationEventData riskEventData;
        private QualityEventData qualityEventData;
        private boolean riskComplete;
        private boolean qualityComplete;
        private Instant firstEventTime;
        
        public BatchEvents() {
            this.riskComplete = false;
            this.qualityComplete = false;
        }
        
        @Override
        public String toString() {
            return String.format(
                "BatchEvents{riskComplete=%s, qualityComplete=%s, firstEventTime=%s}",
                riskComplete, qualityComplete, firstEventTime
            );
        }
    }
}
