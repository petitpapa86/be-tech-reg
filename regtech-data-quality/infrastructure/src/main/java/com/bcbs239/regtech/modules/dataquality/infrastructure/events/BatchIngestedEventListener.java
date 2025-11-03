package com.bcbs239.regtech.modules.dataquality.infrastructure.events;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.dataquality.application.commands.ValidateBatchQualityCommand;
import com.bcbs239.regtech.modules.dataquality.application.commands.ValidateBatchQualityCommandHandler;
import com.bcbs239.regtech.modules.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Specialized event listener for BatchIngested events that triggers quality validation.
 * Includes advanced features like event filtering, routing, and dead letter processing.
 */
@Component
public class BatchIngestedEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchIngestedEventListener.class);
    
    private final ValidateBatchQualityCommandHandler commandHandler;
    private final IQualityReportRepository qualityReportRepository;
    
    // Event processing tracking
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> retryCounters = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastProcessingAttempt = new ConcurrentHashMap<>();
    
    // Statistics
    private final AtomicInteger totalEventsReceived = new AtomicInteger(0);
    private final AtomicInteger totalEventsProcessed = new AtomicInteger(0);
    private final AtomicInteger totalEventsFailed = new AtomicInteger(0);
    private final AtomicInteger totalEventsFiltered = new AtomicInteger(0);
    
    // Configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_COOLDOWN_MINUTES = 5;
    private static final int DEAD_LETTER_THRESHOLD = 5;
    
    public BatchIngestedEventListener(
        ValidateBatchQualityCommandHandler commandHandler,
        IQualityReportRepository qualityReportRepository
    ) {
        this.commandHandler = commandHandler;
        this.qualityReportRepository = qualityReportRepository;
    }
    
    /**
     * Main event handler for BatchIngested events.
     * Includes event filtering, routing logic, and error handling.
     */
    @EventListener
    @Async("qualityEventExecutor")
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = MAX_RETRY_ATTEMPTS, 
               backoff = @Backoff(delay = 2000, multiplier = 2))
    public void handleBatchIngestedEvent(BatchIngestedEvent event) {
        totalEventsReceived.incrementAndGet();
        
        logger.info("Received BatchIngested event: batchId={}, bankId={}, s3Uri={}, expectedCount={}", 
            event.getBatchId(), event.getBankId(), event.getS3Uri(), event.getExpectedExposureCount());
        
        try {
            // Event filtering
            if (!shouldProcessEvent(event)) {
                totalEventsFiltered.incrementAndGet();
                logger.info("Event filtered out for batch: {}", event.getBatchId());
                return;
            }
            
            // Idempotency check
            if (!ensureIdempotency(event)) {
                logger.info("Event already processed or in progress for batch: {}", event.getBatchId());
                return;
            }
            
            // Route event to appropriate handler
            routeEvent(event);
            
            totalEventsProcessed.incrementAndGet();
            logger.info("Successfully processed BatchIngested event for batch: {}", event.getBatchId());
            
        } catch (Exception e) {
            totalEventsFailed.incrementAndGet();
            handleEventProcessingError(event, e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
    
    /**
     * Event filtering logic to determine if an event should be processed.
     */
    private boolean shouldProcessEvent(BatchIngestedEvent event) {
        // Filter out events with invalid data
        if (event.getBatchId() == null || event.getBatchId().trim().isEmpty()) {
            logger.warn("Filtering out event with null or empty batch ID");
            return false;
        }
        
        if (event.getBankId() == null || event.getBankId().trim().isEmpty()) {
            logger.warn("Filtering out event with null or empty bank ID for batch: {}", event.getBatchId());
            return false;
        }
        
        if (event.getS3Uri() == null || event.getS3Uri().trim().isEmpty()) {
            logger.warn("Filtering out event with null or empty S3 URI for batch: {}", event.getBatchId());
            return false;
        }
        
        if (event.getExpectedExposureCount() <= 0) {
            logger.warn("Filtering out event with invalid exposure count for batch: {}", event.getBatchId());
            return false;
        }
        
        // Filter out events that are too old (older than 24 hours)
        if (event.getTimestamp() != null) {
            Instant cutoff = Instant.now().minusSeconds(24 * 60 * 60); // 24 hours ago
            if (event.getTimestamp().isBefore(cutoff)) {
                logger.warn("Filtering out stale event for batch: {} (timestamp: {})", 
                    event.getBatchId(), event.getTimestamp());
                return false;
            }
        }
        
        // Check if batch is in dead letter queue
        if (isInDeadLetterQueue(event.getBatchId())) {
            logger.warn("Filtering out event for batch in dead letter queue: {}", event.getBatchId());
            return false;
        }
        
        return true;
    }
    
    /**
     * Ensure idempotency by checking various sources.
     */
    private boolean ensureIdempotency(BatchIngestedEvent event) {
        String eventKey = createEventKey(event);
        
        // Check if event is already being processed
        if (processedEvents.contains(eventKey)) {
            return false;
        }
        
        // Check if we're in retry cooldown period
        if (isInRetryCooldown(event.getBatchId())) {
            logger.info("Batch {} is in retry cooldown period", event.getBatchId());
            return false;
        }
        
        // Check database for existing quality report
        BatchId batchId = new BatchId(event.getBatchId());
        if (qualityReportRepository.existsByBatchId(batchId)) {
            logger.info("Quality report already exists for batch: {}", event.getBatchId());
            processedEvents.add(eventKey);
            return false;
        }
        
        // Mark as being processed
        processedEvents.add(eventKey);
        lastProcessingAttempt.put(event.getBatchId(), Instant.now());
        
        return true;
    }
    
    /**
     * Route event to appropriate processing logic based on event characteristics.
     */
    private void routeEvent(BatchIngestedEvent event) {
        // Determine processing priority based on file size and bank tier
        ProcessingPriority priority = determinePriority(event);
        
        // Create command with appropriate configuration
        ValidateBatchQualityCommand command = createValidationCommand(event, priority);
        
        // Dispatch command
        Result<Void> result = commandHandler.handle(command);
        
        if (!result.isSuccess()) {
            throw new RuntimeException("Command handling failed: " + result.getError().getMessage());
        }
    }
    
    /**
     * Determine processing priority based on event characteristics.
     */
    private ProcessingPriority determinePriority(BatchIngestedEvent event) {
        // High priority for small files or critical banks
        if (event.getExpectedExposureCount() < 10000) {
            return ProcessingPriority.HIGH;
        }
        
        // Check if bank is marked as critical in metadata
        Map<String, Object> metadata = event.getFileMetadata();
        if (metadata.containsKey("bankTier") && "CRITICAL".equals(metadata.get("bankTier"))) {
            return ProcessingPriority.HIGH;
        }
        
        // Large files get normal priority
        if (event.getExpectedExposureCount() > 100000) {
            return ProcessingPriority.LOW;
        }
        
        return ProcessingPriority.NORMAL;
    }
    
    /**
     * Create validation command with appropriate configuration.
     */
    private ValidateBatchQualityCommand createValidationCommand(BatchIngestedEvent event, ProcessingPriority priority) {
        return new ValidateBatchQualityCommand(
            new BatchId(event.getBatchId()),
            new BankId(event.getBankId()),
            event.getS3Uri(),
            event.getExpectedExposureCount(),
            event.getFileMetadata(),
            priority
        );
    }
    
    /**
     * Handle event processing errors with retry logic and dead letter processing.
     */
    private void handleEventProcessingError(BatchIngestedEvent event, Exception error) {
        String batchId = event.getBatchId();
        
        // Increment retry counter
        int retryCount = retryCounters.merge(batchId, 1, Integer::sum);
        
        logger.error("Error processing BatchIngested event for batch: {} (attempt {})", 
            batchId, retryCount, error);
        
        // Remove from processed set to allow retry
        String eventKey = createEventKey(event);
        processedEvents.remove(eventKey);
        
        // Check if we should move to dead letter queue
        if (retryCount >= DEAD_LETTER_THRESHOLD) {
            moveToDeadLetterQueue(event, error);
        }
    }
    
    /**
     * Move event to dead letter queue for manual processing.
     */
    private void moveToDeadLetterQueue(BatchIngestedEvent event, Exception error) {
        logger.error("Moving batch {} to dead letter queue after {} failed attempts", 
            event.getBatchId(), DEAD_LETTER_THRESHOLD);
        
        // In a real implementation, this would write to a dead letter table or queue
        // For now, we'll just log and clean up tracking data
        
        retryCounters.remove(event.getBatchId());
        lastProcessingAttempt.remove(event.getBatchId());
        
        // TODO: Implement actual dead letter queue storage
        // deadLetterRepository.save(new DeadLetterEvent(event, error));
    }
    
    /**
     * Check if batch is in dead letter queue.
     */
    private boolean isInDeadLetterQueue(String batchId) {
        // TODO: Implement actual dead letter queue check
        // return deadLetterRepository.existsByBatchId(batchId);
        return false;
    }
    
    /**
     * Check if batch is in retry cooldown period.
     */
    private boolean isInRetryCooldown(String batchId) {
        Instant lastAttempt = lastProcessingAttempt.get(batchId);
        if (lastAttempt == null) {
            return false;
        }
        
        Instant cooldownEnd = lastAttempt.plusSeconds(RETRY_COOLDOWN_MINUTES * 60);
        return Instant.now().isBefore(cooldownEnd);
    }
    
    /**
     * Create unique event key for idempotency checking.
     */
    private String createEventKey(BatchIngestedEvent event) {
        return String.format("%s:%s:%s", 
            event.getBatchId(), 
            event.getBankId(), 
            event.getTimestamp() != null ? event.getTimestamp().toString() : "unknown");
    }
    
    /**
     * Get processing statistics for monitoring.
     */
    public EventProcessingStatistics getStatistics() {
        return new EventProcessingStatistics(
            totalEventsReceived.get(),
            totalEventsProcessed.get(),
            totalEventsFailed.get(),
            totalEventsFiltered.get(),
            processedEvents.size(),
            retryCounters.size()
        );
    }
    
    /**
     * Clear processing caches (for maintenance).
     */
    public void clearCaches() {
        processedEvents.clear();
        retryCounters.clear();
        lastProcessingAttempt.clear();
        logger.info("Cleared event processing caches");
    }
    
    /**
     * Processing priority enumeration.
     */
    public enum ProcessingPriority {
        HIGH, NORMAL, LOW
    }
    
    /**
     * Event processing statistics.
     */
    public static class EventProcessingStatistics {
        private final int totalReceived;
        private final int totalProcessed;
        private final int totalFailed;
        private final int totalFiltered;
        private final int currentlyProcessing;
        private final int inRetry;
        
        public EventProcessingStatistics(
            int totalReceived, 
            int totalProcessed, 
            int totalFailed, 
            int totalFiltered,
            int currentlyProcessing, 
            int inRetry
        ) {
            this.totalReceived = totalReceived;
            this.totalProcessed = totalProcessed;
            this.totalFailed = totalFailed;
            this.totalFiltered = totalFiltered;
            this.currentlyProcessing = currentlyProcessing;
            this.inRetry = inRetry;
        }
        
        // Getters
        public int getTotalReceived() { return totalReceived; }
        public int getTotalProcessed() { return totalProcessed; }
        public int getTotalFailed() { return totalFailed; }
        public int getTotalFiltered() { return totalFiltered; }
        public int getCurrentlyProcessing() { return currentlyProcessing; }
        public int getInRetry() { return inRetry; }
        
        public double getSuccessRate() {
            return totalReceived > 0 ? (double) totalProcessed / totalReceived * 100.0 : 0.0;
        }
        
        public double getFailureRate() {
            return totalReceived > 0 ? (double) totalFailed / totalReceived * 100.0 : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "EventProcessingStatistics{received=%d, processed=%d, failed=%d, filtered=%d, " +
                "processing=%d, retry=%d, successRate=%.2f%%, failureRate=%.2f%%}",
                totalReceived, totalProcessed, totalFailed, totalFiltered,
                currentlyProcessing, inRetry, getSuccessRate(), getFailureRate()
            );
        }
    }
}