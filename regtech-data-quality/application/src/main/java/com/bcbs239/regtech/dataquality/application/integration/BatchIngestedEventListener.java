package com.bcbs239.regtech.dataquality.application.integration;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.validation.ValidateBatchQualityCommand;
import com.bcbs239.regtech.dataquality.application.validation.ValidateBatchQualityCommandHandler;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.ingestion.domain.integrationevents.BatchIngestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
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
 * 
 * <p>Event Processing Flow:
 * <ul>
 *   <li>Events are filtered for validity (non-empty IDs, valid counts, not stale)</li>
 *   <li>Idempotency is ensured by checking for existing quality reports</li>
 *   <li>Failed events are persisted to IEventProcessingFailureRepository</li>
 *   <li>EventRetryProcessor automatically retries failed events with exponential backoff</li>
 *   <li>Permanently failed events (after max retries) remain in the repository for manual intervention</li>
 * </ul>
 * 
 * <p>No manual retry logic is needed - the EventRetryProcessor handles all retry attempts
 * and the failure repository serves as the dead letter queue for permanently failed events.
 */
@Component
public class BatchIngestedEventListener {

    private static final Logger log = LoggerFactory.getLogger(BatchIngestedEventListener.class);
    private final ValidateBatchQualityCommandHandler commandHandler;
    private final IQualityReportRepository qualityReportRepository;
    private final IEventProcessingFailureRepository failureRepository;
    private final ObjectMapper objectMapper;

    // Event processing tracking
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    // Statistics
    private final AtomicInteger totalEventsReceived = new AtomicInteger(0);
    private final AtomicInteger totalEventsProcessed = new AtomicInteger(0);
    private final AtomicInteger totalEventsFailed = new AtomicInteger(0);
    private final AtomicInteger totalEventsFiltered = new AtomicInteger(0);

    public BatchIngestedEventListener(
            ValidateBatchQualityCommandHandler commandHandler,
            IQualityReportRepository qualityReportRepository,
            IEventProcessingFailureRepository failureRepository,
            ObjectMapper objectMapper
    ) {
        this.commandHandler = commandHandler;
        this.qualityReportRepository = qualityReportRepository;
        this.failureRepository = failureRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Main event handler for BatchIngested events.
     * Includes event filtering, routing logic, and error handling.
     * Failed events are persisted to the failure repository for retry by EventRetryProcessor.
     * 
     * Note: No @Transactional here - each internal operation manages its own transaction.
     * This prevents "Transaction silently rolled back" errors when handleEventProcessingError()
     * marks the transaction as rollback-only.
     */
    @EventListener
    @Async("qualityEventExecutor")
    public void handleBatchIngestedEvent(BatchIngestedEvent event) {
        totalEventsReceived.incrementAndGet();

        log.info("batch_ingested_event_received; details={}", Map.of(
                "batchId", event.getBatchId(),
                "bankId", event.getBankId(),
                "s3Uri", event.getS3Uri(),
                "totalExposures", String.valueOf(event.getTotalExposures()),
                "isInboxReplay", String.valueOf(CorrelationContext.isInboxReplay())
        ));

        try {
            // Skip processing entirely if this is a replay (either inbox or outbox)
            // Events are processed once during initial dispatch, replays are for reliability only
            if (CorrelationContext.isInboxReplay() || CorrelationContext.isOutboxReplay()) {
                log.info("batch_ingested_event_replay_skip; details={}", Map.of(
                        "batchId", event.getBatchId(),
                        "reason", CorrelationContext.isInboxReplay() ? "inbox_replay_skipped" : "outbox_replay_skipped"
                ));
                return;
            }

            // Event filtering
            if (!shouldProcessEvent(event)) {
                totalEventsFiltered.incrementAndGet();
                log.info("batch_ingested_event_filtered; details={}", Map.of(
                        "batchId", event.getBatchId(),
                        "reason", "failed_validation"
                ));
                return;
            }

            // Idempotency check
            if (!ensureIdempotency(event)) {
                log.info("batch_ingested_event_already_processed; details={}", Map.of(
                        "batchId", event.getBatchId()
                ));
                return;
            }

            // Route event to appropriate handler
            routeEvent(event);

            totalEventsProcessed.incrementAndGet();
            log.info("batch_ingested_event_processed_successfully; details={}", Map.of(
                    "batchId", event.getBatchId()
            ));

        } catch (Exception e) {
            totalEventsFailed.incrementAndGet();
            handleEventProcessingError(event, e);
            // Don't re-throw - error is persisted for retry by EventRetryProcessor
        }
    }

    /**
     * Event filtering logic to determine if an event should be processed.
     */
    private boolean shouldProcessEvent(BatchIngestedEvent event) {
        // Filter out events with invalid data
        if (event.getBatchId() == null || event.getBatchId().trim().isEmpty()) {
            log.info("batch_ingested_event_invalid_batch_id; details={}", Map.of(
                    "reason", "null_or_empty_batch_id"
            ));
            return false;
        }

        if (event.getBankId() == null || event.getBankId().trim().isEmpty()) {
            log.info("batch_ingested_event_invalid_bank_id; details={}", Map.of(
                    "batchId", event.getBatchId(),
                    "reason", "null_or_empty_bank_id"
            ));
            return false;
        }

        if (event.getS3Uri() == null || event.getS3Uri().trim().isEmpty()) {
            log.info("batch_ingested_event_invalid_s3_uri; details={}", Map.of(
                    "batchId", event.getBatchId(),
                    "reason", "null_or_empty_s3_uri"
            ));
            return false;
        }

        if (event.getTotalExposures() <= 0) {
            log.info("batch_ingested_event_invalid_exposure_count; details={}", Map.of(
                    "batchId", event.getBatchId(),
                    "totalExposures", String.valueOf(event.getTotalExposures())
            ));
            return false;
        }

        // Filter out events that are too old (older than 24 hours)
        if (event.getCompletedAt() != null) {
            Instant cutoff = Instant.now().minusSeconds(24 * 60 * 60); // 24 hours ago
            if (event.getCompletedAt().isBefore(cutoff)) {
                log.info("batch_ingested_event_stale; details={}", Map.of(
                        "batchId", event.getBatchId(),
                        "completedAt", event.getCompletedAt().toString(),
                        "cutoff", cutoff.toString()
                ));
                return false;
            }
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

        // Check database for existing quality report
        BatchId batchId = new BatchId(event.getBatchId());
        if (qualityReportRepository.existsByBatchId(batchId)) {
            log.info("batch_quality_report_already_exists; details={}", Map.of(
                    "batchId", event.getBatchId()
            ));
            processedEvents.add(eventKey);
            return false;
        }

        // Mark as being processed
        processedEvents.add(eventKey);

        return true;
    }

    /**
     * Route event to appropriate processing logic based on event characteristics.
     */
    private void routeEvent(BatchIngestedEvent event) {
        // Create command with appropriate configuration
        ValidateBatchQualityCommand command = createValidationCommand(event);

        // Dispatch command
        Result<Void> result = commandHandler.handle(command);

        if (!result.isSuccess()) {
            String errMsg = result.getError().map(ed -> ed.getMessage() != null ? ed.getMessage() : ed.getCode()).orElse("unknown error");
            throw new RuntimeException("Command handling failed: " + errMsg);
        }
    }

    /**
     * Create validation command with appropriate configuration.
     */
    private ValidateBatchQualityCommand createValidationCommand(BatchIngestedEvent event) {
        // Try to reuse a correlation id if available
        String correlationId = event.getCorrelationId();

        if (correlationId != null && !correlationId.isEmpty()) {
            return ValidateBatchQualityCommand.withCorrelation(
                    new BatchId(event.getBatchId()),
                    new BankId(event.getBankId()),
                    event.getS3Uri(),
                    event.getTotalExposures(),
                    correlationId
            );
        }

        return ValidateBatchQualityCommand.of(
                new BatchId(event.getBatchId()),
                new BankId(event.getBankId()),
                event.getS3Uri(),
                event.getTotalExposures()
        );
    }

    /**
     * Handle event processing errors by persisting to the failure repository.
     * 
     * <p>The IEventProcessingFailureRepository serves as the dead letter queue.
     * The EventRetryProcessor will automatically:
     * <ul>
     *   <li>Retry failed events with exponential backoff</li>
     *   <li>Track retry counts and attempt times</li>
     *   <li>Mark events as permanently failed after max retries</li>
     *   <li>Publish integration events for monitoring and alerting</li>
     * </ul>
     * 
     * <p>No manual retry logic or dead letter queue implementation is needed here.
     */
    private void handleEventProcessingError(BatchIngestedEvent event, Exception error) {
        String batchId = event.getBatchId();

        log.error("batch_ingested_event_processing_error; details={}", Map.of(
                "batchId", batchId,
                "bankId", event.getBankId(),
                "s3Uri", event.getS3Uri()
        ), error);

        // Persist failure to repository for retry by EventRetryProcessor
        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            
            Map<String, String> metadata = Map.of(
                "batchId", event.getBatchId(),
                "bankId", event.getBankId(),
                "s3Uri", event.getS3Uri(),
                "totalExposures", String.valueOf(event.getTotalExposures()),
                "correlationId", event.getCorrelationId() != null ? event.getCorrelationId() : "",
                "eventId", event.getEventId() != null ? event.getEventId() : ""
            );
            
            EventProcessingFailure failure = EventProcessingFailure.create(
                event.getClass().getName(),
                eventPayload,
                error.getMessage() != null ? error.getMessage() : error.getClass().getName(),
                getStackTraceAsString(error),
                metadata,
                5 // max retries - will be handled by EventRetryProcessor
            );
            
            failureRepository.save(failure);
            
            log.info("event_processing_failure_persisted; details={}", Map.of(
                    "batchId", batchId,
                    "message", "will_be_retried_by_EventRetryProcessor"
            ));
            
        } catch (Exception saveEx) {
            log.error("failed_to_persist_event_processing_failure; details={}", Map.of(
                    "batchId", batchId
            ), saveEx);
        }

        // Remove from processed set to allow retry
        String eventKey = createEventKey(event);
        processedEvents.remove(eventKey);
    }

    /**
     * Get stack trace as string for error logging.
     */
    private String getStackTraceAsString(Exception e) {
        if (e == null) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * Create unique event key for idempotency checking.
     */
    private String createEventKey(BatchIngestedEvent event) {
        return String.format("%s:%s:%s",
                event.getBatchId(),
                event.getBankId(),
                event.getCompletedAt() != null ? event.getCompletedAt().toString() : "unknown");
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
                processedEvents.size()
        );
    }

    /**
     * Clear processing caches (for maintenance).
     */
    public void clearCaches() {
        processedEvents.clear();
        log.info("event_processing_caches_cleared; details={}", Map.of(
                "action", "cache_maintenance"
        ));
    }

    /**
     * Event processing statistics.
     */
    public record EventProcessingStatistics(
            int totalReceived, 
            int totalProcessed, 
            int totalFailed, 
            int totalFiltered,
            int currentlyProcessing) {

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
                            "processing=%d, successRate=%.2f%%, failureRate=%.2f%%}",
                    totalReceived, totalProcessed, totalFailed, totalFiltered,
                    currentlyProcessing, getSuccessRate(), getFailureRate()
            );
        }
    }
}

