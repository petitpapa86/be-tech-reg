package com.bcbs239.regtech.riskcalculation.application.integration;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.integrationevents.BatchIngestedEvent;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculateRiskMetricsCommand;
import com.bcbs239.regtech.riskcalculation.application.calculation.CalculateRiskMetricsCommandHandler;
import com.bcbs239.regtech.riskcalculation.domain.calculation.IBatchSummaryRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Event listener for BatchIngestedEvent from the ingestion module.
 * Handles async processing with idempotency checking and error handling.
 * 
 * Features:
 * - Async processing using dedicated thread pool
 * - Idempotency checking to prevent duplicate processing
 * - Event filtering and validation
 * - Error handling with EventProcessingFailure repository
 * - Structured logging for monitoring
 */
@Component("riskCalculationBatchIngestedEventListener")
@RequiredArgsConstructor
@Slf4j
public class BatchIngestedEventListener {
    
    private final CalculateRiskMetricsCommandHandler commandHandler;
    private final IBatchSummaryRepository batchSummaryRepository;
    private final IEventProcessingFailureRepository failureRepository;
    private final ILogger asyncLogger;
    private final ObjectMapper objectMapper;
    
    /**
     * Handles BatchIngestedEvent with async processing and comprehensive error handling.
     * 
     * @param event The batch ingested event from ingestion module
     */
    @EventListener
    @Async("riskCalculationExecutor")
    @Transactional
    public void handleBatchIngestedEvent(BatchIngestedEvent event) {
        log.info("Received BatchIngestedEvent for batch: {} from bank: {}", 
            event.getBatchId(), event.getBankId());
        
        try {
            // Step 1: Validate event
            if (!isValidEvent(event)) {
                log.warn("Invalid BatchIngestedEvent received, skipping processing: {}", event);
                return;
            }
            
            // Step 2: Check idempotency - skip if already processed
            BatchId batchId = BatchId.of(event.getBatchId());
            if (batchSummaryRepository.existsByBatchId(batchId)) {
                log.info("Batch {} already processed, skipping duplicate event", event.getBatchId());
                return;
            }
            
            // Step 3: Create and execute risk calculation command
            Result<CalculateRiskMetricsCommand> commandResult = CalculateRiskMetricsCommand.create(
                event.getBatchId(),
                event.getBankId(),
                event.getS3Uri(),
                event.getTotalExposures(),
                null // No correlation ID from ingestion event
            );
            
            if (commandResult.isFailure()) {
                log.error("Failed to create CalculateRiskMetricsCommand for batch: {}", event.getBatchId());
                handleEventProcessingError(event, commandResult.getError().get().getMessage(), null);
                return;
            }
            
            // Step 4: Execute risk calculation
            Result<Void> executionResult = commandHandler.handle(commandResult.getValue().get());
            
            if (executionResult.isFailure()) {
                log.error("Risk calculation failed for batch: {}", event.getBatchId());
                handleEventProcessingError(event, executionResult.getError().get().getMessage(), null);
                return;
            }
            
            log.info("Successfully processed BatchIngestedEvent for batch: {}", event.getBatchId());
            
        } catch (Exception e) {
            log.error("Unexpected error processing BatchIngestedEvent for batch: {}", 
                event.getBatchId(), e);
            handleEventProcessingError(event, "Unexpected error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates the incoming BatchIngestedEvent for required fields and business rules.
     * 
     * @param event The event to validate
     * @return true if event is valid, false otherwise
     */
    private boolean isValidEvent(BatchIngestedEvent event) {
        if (event == null) {
            log.warn("Received null BatchIngestedEvent");
            return false;
        }
        
        if (event.getBatchId() == null || event.getBatchId().trim().isEmpty()) {
            log.warn("BatchIngestedEvent has null or empty batch ID");
            return false;
        }
        
        if (event.getBankId() == null || event.getBankId().trim().isEmpty()) {
            log.warn("BatchIngestedEvent has null or empty bank ID");
            return false;
        }
        
        if (event.getS3Uri() == null || event.getS3Uri().trim().isEmpty()) {
            log.warn("BatchIngestedEvent has null or empty S3 URI");
            return false;
        }
        
        if (event.getTotalExposures() <= 0) {
            log.warn("BatchIngestedEvent has invalid total exposures count: {}", event.getTotalExposures());
            return false;
        }
        
        // Check if event is stale (older than 24 hours)
        if (event.getCompletedAt() != null && 
            event.getCompletedAt().isBefore(Instant.now().minusSeconds(86400))) {
            log.warn("BatchIngestedEvent is stale (older than 24 hours): {}", event.getCompletedAt());
            return false;
        }
        
        return true;
    }
    
    /**
     * Handles event processing errors by persisting to the failure repository.
     * This serves as a dead letter queue for failed events that can be retried later.
     * 
     * @param event The failed event
     * @param errorMessage The error message
     * @param exception The exception (if any)
     */
    private void handleEventProcessingError(BatchIngestedEvent event, String errorMessage, Exception exception) {
        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            String stackTrace = exception != null ? getStackTrace(exception) : null;
            
            Map<String, String> metadata = Map.of(
                "batchId", String.valueOf(event.getBatchId()),
                "bankId", String.valueOf(event.getBankId()),
                "s3Uri", String.valueOf(event.getS3Uri()),
                "totalExposures", String.valueOf(event.getTotalExposures()),
                "eventVersion", String.valueOf(event.getEventVersion())
            );
            
            EventProcessingFailure failure = EventProcessingFailure.createWithMetadata(
                event.getClass().getName(),
                eventPayload,
                metadata,
                errorMessage,
                stackTrace,
                3 // maxRetries
            );
            
            Result<EventProcessingFailure> saveResult = failureRepository.save(failure);
            if (saveResult.isFailure()) {
                log.error("Failed to save event processing failure for batch: {}", event.getBatchId());
            } else {
                log.info("Saved event processing failure for batch: {} with ID: {}", 
                    event.getBatchId(), failure.getId());
            }
            
        } catch (Exception e) {
            log.error("Failed to handle event processing error for batch: {}", event.getBatchId(), e);
        }
    }
    
    /**
     * Extracts stack trace from exception
     */
    private String getStackTrace(Exception exception) {
        if (exception == null) return null;
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}