package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.core.domain.eventprocessing.EventProcessingFailure;
import com.bcbs239.regtech.core.domain.eventprocessing.IEventProcessingFailureRepository;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.batch.events.BatchProcessingFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles batch processing failures by saving them to the event processing failure table
 * for automatic retry by EventRetryProcessor.
 */
@Component
@Slf4j
public class BatchProcessingFailureHandler {
    
    private final IEventProcessingFailureRepository failureRepository;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    
    public BatchProcessingFailureHandler(
            IEventProcessingFailureRepository failureRepository,
            ObjectMapper objectMapper,
            @Value("${ingestion.retry.max-retries:5}") int maxRetries) {
        this.failureRepository = failureRepository;
        this.objectMapper = objectMapper;
        this.maxRetries = maxRetries;
    }
    
    @EventListener
    @Transactional
    public void handleBatchProcessingFailed(BatchProcessingFailedEvent event) {
        try {
            // Serialize the event to JSON
            String eventPayload = objectMapper.writeValueAsString(event);
            
            // Build metadata map
            Map<String, String> metadata = new HashMap<>();
            metadata.put("batchId", event.getBatchId());
            metadata.put("bankId", event.getBankId());
            metadata.put("fileName", event.getFileName());
            metadata.put("errorType", event.getErrorType());
            metadata.put("tempFileKey", event.getTempFileKey());
            metadata.put("eventId", event.getEventId());
            metadata.put("correlationId", event.getCorrelationId());
            
            // Create failure record
            EventProcessingFailure failure = EventProcessingFailure.createWithMetadata(
                event.getClass().getName(),
                eventPayload,
                metadata,
                event.getErrorMessage(),
                "", // No stack trace at this point
                maxRetries
            );
            
            // Save to repository
            Result<EventProcessingFailure> saveResult = failureRepository.save(failure);
            
            if (saveResult.isSuccess()) {
                log.info("Batch processing failure saved for retry; details={}", Map.of(
                    "batchId", event.getBatchId(),
                    "bankId", event.getBankId(),
                    "fileName", event.getFileName(),
                    "failureId", failure.getId()
                ));
            } else {
                log.error("Failed to save batch processing failure; details={}", Map.of(
                    "batchId", event.getBatchId(),
                    "error", saveResult.getError().map(e -> e.getMessage()).orElse("Unknown error")
                ));
            }
            
        } catch (Exception e) {
            log.error("Error handling batch processing failure event; details={}", Map.of(
                "batchId", event.getBatchId(),
                "errorMessage", e.getMessage()
            ), e);
        }
    }
}
