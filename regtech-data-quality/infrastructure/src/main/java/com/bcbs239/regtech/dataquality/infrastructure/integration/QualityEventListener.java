package com.bcbs239.regtech.dataquality.infrastructure.integration;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.application.validation.ValidateBatchQualityCommand;
import com.bcbs239.regtech.dataquality.application.validation.ValidateBatchQualityCommandHandler;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for handling BatchIngested events from the CrossModuleEventBus.
 * Triggers quality validation processing when new batches are ingested.
 */
@Component
public class QualityEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityEventListener.class);
    
    private final ValidateBatchQualityCommandHandler commandHandler;
    private final IQualityReportRepository qualityReportRepository;
    
    // Track processed batches to ensure idempotency
    private final Set<String> processedBatches = ConcurrentHashMap.newKeySet();
    
    public QualityEventListener(
        ValidateBatchQualityCommandHandler commandHandler,
        IQualityReportRepository qualityReportRepository
    ) {
        this.commandHandler = commandHandler;
        this.qualityReportRepository = qualityReportRepository;
    }
    
    /**
     * Handle BatchIngested events from the ingestion module.
     * This method is called asynchronously to avoid blocking the event publisher.
     */
    @EventListener
    @Async("qualityEventExecutor")
    @Transactional
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void handleBatchIngestedEvent(BatchIngestedEvent event) {
        logger.info("Received BatchIngested event for batch: {} from bank: {}", 
            event.getBatchId(), event.getBankId());
        
        try {
            // Check for idempotency - ensure we don't process the same batch twice
            if (!ensureIdempotency(event.getBatchId())) {
                logger.info("Batch {} already processed or in progress, skipping", event.getBatchId());
                return;
            }
            
            // Create and execute validation command (use factory; fileMetadata not part of command)
            ValidateBatchQualityCommand command = ValidateBatchQualityCommand.of(
                new BatchId(event.getBatchId()),
                new BankId(event.getBankId()),
                event.getS3Uri(),
                event.getExpectedExposureCount()
            );
            
            Result<Void> result = commandHandler.handle(command);

             if (result.isSuccess()) {
                 logger.info("Successfully initiated quality validation for batch: {}", event.getBatchId());
             } else {
                String errMsg = result.getError().map(e -> e.getMessage() != null ? e.getMessage() : e.getCode()).orElse("unknown error");
                logger.error("Failed to initiate quality validation for batch: {} - Error: {}", event.getBatchId(), errMsg);

                 // Remove from processed set so it can be retried
                 processedBatches.remove(event.getBatchId());

                 // Re-throw to trigger retry mechanism
                throw new RuntimeException("Quality validation failed: " + errMsg);
             }

        } catch (Exception e) {
            logger.error("Error processing BatchIngested event for batch: {}", event.getBatchId(), e);
            
            // Remove from processed set so it can be retried
            processedBatches.remove(event.getBatchId());
            
            // Re-throw to trigger retry mechanism
            throw e;
        }
    }
    
    /**
     * Handle BatchProcessingFailed events to clean up any partial processing.
     */
    @EventListener
    @Async("qualityEventExecutor")
    @Transactional
    public void handleBatchProcessingFailedEvent(BatchProcessingFailedEvent event) {
        logger.info("Received BatchProcessingFailed event for batch: {}", event.getBatchId());
        
        try {
            // Remove from processed set to allow reprocessing if the batch is resubmitted
            processedBatches.remove(event.getBatchId());
            
            // Check if we have a quality report for this batch and mark it as failed
            BatchId batchId = new BatchId(event.getBatchId());
            var qualityReport = qualityReportRepository.findByBatchId(batchId);
            
            if (qualityReport.isPresent()) {
                var report = qualityReport.get();
                Result<Void> markFailedResult = report.markAsFailed(
                    "Batch processing failed: " + event.getErrorMessage()
                );
                
                if (markFailedResult.isSuccess()) {
                    qualityReportRepository.save(report);
                    logger.info("Marked quality report as failed for batch: {}", event.getBatchId());
                } else {
                    logger.error("Failed to mark quality report as failed for batch: {}", event.getBatchId());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error handling BatchProcessingFailed event for batch: {}", event.getBatchId(), e);
        }
    }
    
    /**
     * Ensure idempotency by checking if the batch has already been processed.
     * Also checks the database to see if a quality report already exists.
     */
    private boolean ensureIdempotency(String batchId) {
        // Check in-memory cache first
        if (processedBatches.contains(batchId)) {
            return false;
        }
        
        // Check database for existing quality report
        BatchId batchIdObj = new BatchId(batchId);
        if (qualityReportRepository.existsByBatchId(batchIdObj)) {
            logger.info("Quality report already exists for batch: {}", batchId);
            processedBatches.add(batchId);
            return false;
        }
        
        // Mark as being processed
        processedBatches.add(batchId);
        return true;
    }
    
    /**
     * Get the count of currently processed batches (for monitoring).
     */
    public int getProcessedBatchCount() {
        return processedBatches.size();
    }
    
    /**
     * Clear the processed batches cache (for testing or maintenance).
     */
    public void clearProcessedBatchesCache() {
        processedBatches.clear();
        logger.info("Cleared processed batches cache");
    }
    
    /**
     * Check if a batch is currently being processed.
     */
    public boolean isBatchBeingProcessed(String batchId) {
        return processedBatches.contains(batchId);
    }
}

/**
 * Event class representing a batch that has been successfully ingested.
 * This event is published by the ingestion module.
 */
@Getter
class BatchIngestedEvent {
    // Getters
    private final String batchId;
    private final String bankId;
    private final String s3Uri;
    private final int expectedExposureCount;
    private final Map<String, Object> fileMetadata;
    private final java.time.Instant timestamp;
    
    public BatchIngestedEvent(
        String batchId, 
        String bankId, 
        String s3Uri, 
        int expectedExposureCount,
        Map<String, Object> fileMetadata
    ) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3Uri = s3Uri;
        this.expectedExposureCount = expectedExposureCount;
        this.fileMetadata = fileMetadata != null ? fileMetadata : new java.util.HashMap<>();
        this.timestamp = java.time.Instant.now();
    }

    @Override
    public String toString() {
        return "BatchIngestedEvent{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", s3Uri='" + s3Uri + '\'' +
                ", expectedExposureCount=" + expectedExposureCount +
                ", timestamp=" + timestamp +
                '}';
    }
}

/**
 * Event class representing a batch processing failure.
 */
@Getter
class BatchProcessingFailedEvent {
    // Getters
    private final String batchId;
    private final String bankId;
    private final String errorMessage;
    private final java.time.Instant timestamp;
    
    public BatchProcessingFailedEvent(String batchId, String bankId, String errorMessage) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.errorMessage = errorMessage;
        this.timestamp = java.time.Instant.now();
    }

    @Override
    public String toString() {
        return "BatchProcessingFailedEvent{" +
                "batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

