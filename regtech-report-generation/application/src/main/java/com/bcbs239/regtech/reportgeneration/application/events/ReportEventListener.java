package com.bcbs239.regtech.reportgeneration.application.events;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.dataquality.application.integration.events.BatchQualityCompletedEvent;
import com.bcbs239.regtech.reportgeneration.application.coordination.CalculationEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.QualityEventData;
import com.bcbs239.regtech.reportgeneration.application.coordination.ReportCoordinator;
import com.bcbs239.regtech.reportgeneration.domain.generation.IGeneratedReportRepository;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportStatus;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Event listener for batch calculation and quality completion events.
 * 
 * <p>This listener implements the event-driven coordination pattern required by Requirements 1.1-1.7, 2.2-2.4.
 * It validates incoming events, checks for idempotency, detects stale events, and delegates to
 * the ReportCoordinator for dual-event coordination.
 * 
 * <p>Event Processing Flow:
 * <ul>
 *   <li>Events are filtered for validity (non-empty IDs, valid URIs, not stale)</li>
 *   <li>Idempotency is ensured by checking for existing COMPLETED reports</li>
 *   <li>Failed events are persisted to IEventProcessingFailureRepository</li>
 *   <li>EventRetryProcessor automatically retries failed events with exponential backoff</li>
 *   <li>Permanently failed events (after max retries) remain in the repository for manual intervention</li>
 * </ul>
 * 
 * <p>Key responsibilities:
 * <ul>
 *   <li>Listen to BatchCalculationCompletedEvent from Risk Calculation Module</li>
 *   <li>Listen to BatchQualityCompletedEvent from Data Quality Module</li>
 *   <li>Validate event data and reject invalid/stale events</li>
 *   <li>Check idempotency to prevent duplicate processing</li>
 *   <li>Delegate to ReportCoordinator for event coordination</li>
 *   <li>Handle errors by persisting EventProcessingFailure records for automatic retry</li>
 * </ul>
 * 
 * <p>Design decisions:
 * <ul>
 *   <li>Uses @Async with named executor for non-blocking event processing</li>
 *   <li>Implements stale event detection (>24 hours) to prevent processing outdated data</li>
 *   <li>Persists failures to EventProcessingFailure repository for automatic retry by EventRetryProcessor</li>
 *   <li>Does not throw exceptions to avoid rolling back upstream transactions</li>
 * </ul>
 * 
 * <p>No manual retry logic is needed - the EventRetryProcessor handles all retry attempts
 * and the failure repository serves as the dead letter queue for permanently failed events.
 */
@Component
@Slf4j
public class ReportEventListener {
    
    private static final Duration STALE_EVENT_THRESHOLD = Duration.ofHours(24);
    private static final int MAX_RETRIES = 5;
    
    private final ReportCoordinator reportCoordinator;
    private final IGeneratedReportRepository reportRepository;
    private final IEventProcessingFailureRepository failureRepository;
    private final ObjectMapper objectMapper;
    
    // Event processing tracking
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();
    
    // Statistics
    private final AtomicInteger totalCalculationEventsReceived = new AtomicInteger(0);
    private final AtomicInteger totalQualityEventsReceived = new AtomicInteger(0);
    private final AtomicInteger totalEventsProcessed = new AtomicInteger(0);
    private final AtomicInteger totalEventsFailed = new AtomicInteger(0);
    private final AtomicInteger totalEventsFiltered = new AtomicInteger(0);
    
    public ReportEventListener(
            ReportCoordinator reportCoordinator,
            IGeneratedReportRepository reportRepository,
            IEventProcessingFailureRepository failureRepository,
            ObjectMapper objectMapper) {
        
        this.reportCoordinator = reportCoordinator;
        this.reportRepository = reportRepository;
        this.failureRepository = failureRepository;
        this.objectMapper = objectMapper;
        
        log.info("ReportEventListener initialized");
    }
    
    /**
     * Handles BatchCalculationCompletedEvent from Risk Calculation Module.
     * 
     * <p>Validates the event, checks for idempotency, detects stale events,
     * and delegates to ReportCoordinator if all checks pass.
     * 
     * <p>Failed events are persisted to the failure repository for retry by EventRetryProcessor.
     * 
     * <p>Requirements: 1.1, 1.2, 1.3, 1.6, 1.7, 2.2, 2.3
     * 
     * @param event the calculation completed event
     */
    @EventListener
    @Async("reportGenerationExecutor")
    @Transactional
    public void handleBatchCalculationCompleted(BatchCalculationCompletedEvent event) {
        totalCalculationEventsReceived.incrementAndGet();
        
        String batchId = event.getBatchId().value();
        
        log.info("batch_calculation_completed_event_received; details={}", Map.of(
                "batchId", batchId,
                "bankId", event.getBankId().value(),
                "resultFileUri", event.getResultFileUri().uri(),
                "totalExposures", String.valueOf(event.getTotalExposures().count())
        ));
        
        try {
            // Event filtering
            if (!shouldProcessCalculationEvent(event)) {
                totalEventsFiltered.incrementAndGet();
                log.info("batch_calculation_event_filtered; details={}", Map.of(
                        "batchId", batchId,
                        "reason", "failed_validation"
                ));
                return;
            }
            
            // Idempotency check
            if (!ensureIdempotency(batchId)) {
                log.info("batch_calculation_event_already_processed; details={}", Map.of(
                        "batchId", batchId
                ));
                return;
            }
            
            // Route event to coordinator
            CalculationEventData eventData = mapToCalculationEventData(event);
            reportCoordinator.handleCalculationCompleted(eventData);
            
            totalEventsProcessed.incrementAndGet();
            log.info("batch_calculation_event_processed_successfully; details={}", Map.of(
                    "batchId", batchId
            ));
            
        } catch (Exception e) {
            totalEventsFailed.incrementAndGet();
            handleEventProcessingError(event, e);
            // Don't re-throw - error is persisted for retry by EventRetryProcessor
        }
    }
    
    /**
     * Handles BatchQualityCompletedEvent from Data Quality Module.
     * 
     * <p>Validates the event, checks for idempotency, detects stale events,
     * and delegates to ReportCoordinator if all checks pass.
     * 
     * <p>Failed events are persisted to the failure repository for retry by EventRetryProcessor.
     * 
     * <p>Requirements: 1.1, 1.2, 1.3, 1.6, 1.7, 2.2, 2.3
     * 
     * @param event the quality completed event
     */
    @EventListener
    @Async("reportGenerationExecutor")
    @Transactional
    public void handleBatchQualityCompleted(BatchQualityCompletedEvent event) {
        totalQualityEventsReceived.incrementAndGet();
        
        String batchId = event.getBatchId().value();
        
        log.info("batch_quality_completed_event_received; details={}", Map.of(
                "batchId", batchId,
                "bankId", event.getBankId().value(),
                "resultFileUri", event.getS3Reference().uri(),
                "overallScore", String.valueOf(event.getQualityScores().overallScore())
        ));
        
        try {
            // Event filtering
            if (!shouldProcessQualityEvent(event)) {
                totalEventsFiltered.incrementAndGet();
                log.info("batch_quality_event_filtered; details={}", Map.of(
                        "batchId", batchId,
                        "reason", "failed_validation"
                ));
                return;
            }
            
            // Idempotency check
            if (!ensureIdempotency(batchId)) {
                log.info("batch_quality_event_already_processed; details={}", Map.of(
                        "batchId", batchId
                ));
                return;
            }
            
            // Route event to coordinator
            QualityEventData eventData = mapToQualityEventData(event);
            reportCoordinator.handleQualityCompleted(eventData);
            
            totalEventsProcessed.incrementAndGet();
            log.info("batch_quality_event_processed_successfully; details={}", Map.of(
                    "batchId", batchId
            ));
            
        } catch (Exception e) {
            totalEventsFailed.incrementAndGet();
            handleEventProcessingError(event, e);
            // Don't re-throw - error is persisted for retry by EventRetryProcessor
        }
    }
    
    /**
     * Event filtering logic to determine if a calculation event should be processed.
     * Checks for null values, empty strings, and stale timestamps.
     * 
     * @param event the event to validate
     * @return true if should process, false otherwise
     */
    private boolean shouldProcessCalculationEvent(BatchCalculationCompletedEvent event) {
        if (event == null) {
            log.info("batch_calculation_event_invalid; details={}", Map.of(
                    "reason", "null_event"
            ));
            return false;
        }
        
        if (event.getBatchId() == null || event.getBatchId().value() == null || event.getBatchId().value().trim().isEmpty()) {
            log.info("batch_calculation_event_invalid; details={}", Map.of(
                    "reason", "null_or_empty_batch_id"
            ));
            return false;
        }
        
        String batchId = event.getBatchId().value();
        
        if (event.getBankId() == null || event.getBankId().value() == null || event.getBankId().value().trim().isEmpty()) {
            log.info("batch_calculation_event_invalid; details={}", Map.of(
                    "batchId", batchId,
                    "reason", "null_or_empty_bank_id"
            ));
            return false;
        }
        
        if (event.getResultFileUri() == null || event.getResultFileUri().uri() == null || event.getResultFileUri().uri().trim().isEmpty()) {
            log.info("batch_calculation_event_invalid; details={}", Map.of(
                    "batchId", batchId,
                    "reason", "null_or_empty_result_file_uri"
            ));
            return false;
        }
        
        if (event.getCompletedAt() == null) {
            log.info("batch_calculation_event_invalid; details={}", Map.of(
                    "batchId", batchId,
                    "reason", "null_completed_at"
            ));
            return false;
        }
        
        // Check for stale events (older than 24 hours)
        if (isStaleEvent(event.getCompletedAt())) {
            log.info("batch_calculation_event_stale; details={}", Map.of(
                    "batchId", batchId,
                    "completedAt", event.getCompletedAt().toString(),
                    "cutoff", Instant.now().minus(STALE_EVENT_THRESHOLD).toString()
            ));
            return false;
        }
        
        return true;
    }
    
    /**
     * Event filtering logic to determine if a quality event should be processed.
     * Checks for null values, empty strings, and stale timestamps.
     * 
     * @param event the event to validate
     * @return true if should process, false otherwise
     */
    private boolean shouldProcessQualityEvent(BatchQualityCompletedEvent event) {
        if (event == null) {
            log.info("batch_quality_event_invalid; details={}", Map.of(
                    "reason", "null_event"
            ));
            return false;
        }
        
        if (event.getBatchId() == null || event.getBatchId().value() == null || event.getBatchId().value().trim().isEmpty()) {
            log.info("batch_quality_event_invalid; details={}", Map.of(
                    "reason", "null_or_empty_batch_id"
            ));
            return false;
        }
        
        String batchId = event.getBatchId().value();
        
        if (event.getBankId() == null || event.getBankId().value() == null || event.getBankId().value().trim().isEmpty()) {
            log.info("batch_quality_event_invalid; details={}", Map.of(
                    "batchId", batchId,
                    "reason", "null_or_empty_bank_id"
            ));
            return false;
        }
        
        if (event.getS3Reference() == null || event.getS3Reference().uri() == null || event.getS3Reference().uri().trim().isEmpty()) {
            log.info("batch_quality_event_invalid; details={}", Map.of(
                    "batchId", batchId,
                    "reason", "null_or_empty_s3_reference"
            ));
            return false;
        }
        
        if (event.getQualityScores() == null) {
            log.info("batch_quality_event_invalid; details={}", Map.of(
                    "batchId", batchId,
                    "reason", "null_quality_scores"
            ));
            return false;
        }
        
        if (event.getTimestamp() == null) {
            log.info("batch_quality_event_invalid; details={}", Map.of(
                    "batchId", batchId,
                    "reason", "null_timestamp"
            ));
            return false;
        }
        
        // Check for stale events (older than 24 hours)
        if (isStaleEvent(event.getTimestamp())) {
            log.info("batch_quality_event_stale; details={}", Map.of(
                    "batchId", batchId,
                    "timestamp", event.getTimestamp().toString(),
                    "cutoff", Instant.now().minus(STALE_EVENT_THRESHOLD).toString()
            ));
            return false;
        }
        
        return true;
    }
    
    /**
     * Ensure idempotency by checking various sources.
     * 
     * <p>Checks:
     * <ul>
     *   <li>If event is already being processed (in-memory cache)</li>
     *   <li>If report already exists with COMPLETED status (database)</li>
     * </ul>
     * 
     * <p>Requirement 1.2, 2.3
     * 
     * @param batchId the batch identifier
     * @return true if should process, false if already processed
     */
    private boolean ensureIdempotency(String batchId) {
        // Check if event is already being processed
        if (processedEvents.contains(batchId)) {
            return false;
        }
        
        // Check database for existing COMPLETED report
        if (reportRepository.existsByBatchIdAndStatus(BatchId.of(batchId), ReportStatus.COMPLETED)) {
            log.info("report_already_exists; details={}", Map.of(
                    "batchId", batchId,
                    "status", "COMPLETED"
            ));
            processedEvents.add(batchId);
            return false;
        }
        
        // Mark as being processed
        processedEvents.add(batchId);
        
        return true;
    }
    
    /**
     * Checks if an event is stale (older than 24 hours).
     * Stale events are rejected to prevent processing outdated data.
     * 
     * <p>Requirement 1.6
     * 
     * @param completedAt the event completion timestamp
     * @return true if stale, false otherwise
     */
    private boolean isStaleEvent(Instant completedAt) {
        if (completedAt == null) {
            return false; // Will be caught by validation
        }
        
        Duration age = Duration.between(completedAt, Instant.now());
        return age.compareTo(STALE_EVENT_THRESHOLD) > 0;
    }
    
    /**
     * Maps BatchCalculationCompletedEvent to internal CalculationEventData DTO.
     * This decouples the coordination logic from external event structures.
     * 
     * @param event the external event
     * @return the internal DTO
     */
    private CalculationEventData mapToCalculationEventData(BatchCalculationCompletedEvent event) {
        return new CalculationEventData(
            event.getBatchId().value(),
            event.getBankId().value(),
            event.getResultFileUri().uri(),
            event.getTotalExposures().count(),
            event.getTotalAmountEur().value(),
            event.getCompletedAt()
        );
    }
    
    /**
     * Maps BatchQualityCompletedEvent to internal QualityEventData DTO.
     * This decouples the coordination logic from external event structures.
     * 
     * @param event the external event
     * @return the internal DTO
     */
    private QualityEventData mapToQualityEventData(BatchQualityCompletedEvent event) {
        return new QualityEventData(
            event.getBatchId().value(),
            event.getBankId().value(),
            event.getS3Reference().uri(),
            BigDecimal.valueOf(event.getQualityScores().overallScore()),
            event.getQualityScores().grade().name(),
            event.getTimestamp()
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
     * 
     * <p>Requirements: 1.7, 2.4
     * 
     * @param event the event that failed processing
     * @param error the exception that occurred
     */
    private void handleEventProcessingError(Object event, Exception error) {
        String batchId = extractBatchId(event);
        
        log.error("event_processing_error; details={}", Map.of(
                "batchId", batchId,
                "eventType", event.getClass().getSimpleName()
        ), error);
        
        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            
            Map<String, String> metadata = Map.of(
                "batchId", batchId,
                "eventType", event.getClass().getName(),
                "errorType", error.getClass().getSimpleName()
            );
            
            EventProcessingFailure failure = EventProcessingFailure.create(
                event.getClass().getName(),
                eventPayload,
                error.getMessage() != null ? error.getMessage() : error.getClass().getName(),
                getStackTraceAsString(error),
                metadata,
                MAX_RETRIES
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
        processedEvents.remove(batchId);
    }
    
    /**
     * Extracts batch ID from event object using reflection.
     * 
     * @param event the event object
     * @return the batch ID, or "unknown" if extraction fails
     */
    private String extractBatchId(Object event) {
        try {
            if (event instanceof BatchCalculationCompletedEvent) {
                return ((BatchCalculationCompletedEvent) event).getBatchId().value();
            } else if (event instanceof BatchQualityCompletedEvent) {
                return ((BatchQualityCompletedEvent) event).getBatchId().value();
            }
        } catch (Exception e) {
            log.warn("Failed to extract batchId from event", e);
        }
        return "unknown";
    }
    
    /**
     * Converts exception stack trace to string.
     * 
     * @param e the exception
     * @return the stack trace as string
     */
    private String getStackTraceAsString(Throwable e) {
        if (e == null) return "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Get processing statistics for monitoring.
     * 
     * @return event processing statistics
     */
    public EventProcessingStatistics getStatistics() {
        return new EventProcessingStatistics(
                totalCalculationEventsReceived.get(),
                totalQualityEventsReceived.get(),
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
            int totalCalculationEventsReceived,
            int totalQualityEventsReceived,
            int totalEventsProcessed,
            int totalEventsFailed,
            int totalEventsFiltered,
            int currentlyProcessing) {
        
        public int getTotalEventsReceived() {
            return totalCalculationEventsReceived + totalQualityEventsReceived;
        }
        
        public double getSuccessRate() {
            int total = getTotalEventsReceived();
            return total > 0 ? (double) totalEventsProcessed / total * 100.0 : 0.0;
        }
        
        public double getFailureRate() {
            int total = getTotalEventsReceived();
            return total > 0 ? (double) totalEventsFailed / total * 100.0 : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                    "EventProcessingStatistics{calculationReceived=%d, qualityReceived=%d, " +
                            "processed=%d, failed=%d, filtered=%d, processing=%d, " +
                            "successRate=%.2f%%, failureRate=%.2f%%}",
                    totalCalculationEventsReceived, totalQualityEventsReceived,
                    totalEventsProcessed, totalEventsFailed, totalEventsFiltered,
                    currentlyProcessing, getSuccessRate(), getFailureRate()
            );
        }
    }
}
