package com.bcbs239.regtech.modules.dataquality.infrastructure.events;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.dataquality.application.services.CrossModuleEventPublisher;
import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.modules.dataquality.domain.shared.S3Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of CrossModuleEventPublisher that publishes quality events
 * to other modules through the Spring ApplicationEventPublisher.
 */
@Service
public class CrossModuleEventPublisherImpl implements CrossModuleEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(CrossModuleEventPublisherImpl.class);
    
    private final ApplicationEventPublisher eventPublisher;
    private final ExecutorService eventExecutor;
    
    public CrossModuleEventPublisherImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.eventExecutor = Executors.newFixedThreadPool(3);
    }
    
    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<Void> publishBatchQualityCompleted(BatchQualityCompletedEvent event) {
        logger.info("Publishing BatchQualityCompleted event for batch: {} with overall score: {}", 
            event.getBatchId().getValue(), event.getQualityScores().overallScore());
        
        try {
            // Create the event object
            BatchQualityCompletedEventImpl eventImpl = new BatchQualityCompletedEventImpl(
                event.getBatchId(),
                event.getBankId(),
                event.getQualityScores(),
                event.getS3Reference(),
                event.getValidationSummary(),
                event.getProcessingMetadata()
            );
            
            // Publish the event
            eventPublisher.publishEvent(eventImpl);
            
            logger.info("Successfully published BatchQualityCompleted event for batch: {}", 
                event.getBatchId().getValue());
            
            return Result.success();
            
        } catch (Exception e) {
            logger.error("Failed to publish BatchQualityCompleted event for batch: {}", 
                event.getBatchId().getValue(), e);
            
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISH_ERROR",
                "Failed to publish BatchQualityCompleted event: " + e.getMessage(),
                "event_publishing"
            ));
        }
    }
    
    @Override
    public CompletableFuture<Result<Void>> publishBatchQualityCompletedAsync(BatchQualityCompletedEvent event) {
        return CompletableFuture.supplyAsync(() -> publishBatchQualityCompleted(event), eventExecutor);
    }
    
    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<Void> publishBatchQualityFailed(BatchQualityFailedEvent event) {
        logger.info("Publishing BatchQualityFailed event for batch: {} with error: {}", 
            event.getBatchId().getValue(), event.getErrorMessage());
        
        try {
            // Create the event object
            BatchQualityFailedEventImpl eventImpl = new BatchQualityFailedEventImpl(
                event.getBatchId(),
                event.getBankId(),
                event.getErrorMessage(),
                event.getErrorDetails(),
                event.getProcessingMetadata()
            );
            
            // Publish the event
            eventPublisher.publishEvent(eventImpl);
            
            logger.info("Successfully published BatchQualityFailed event for batch: {}", 
                event.getBatchId().getValue());
            
            return Result.success();
            
        } catch (Exception e) {
            logger.error("Failed to publish BatchQualityFailed event for batch: {}", 
                event.getBatchId().getValue(), e);
            
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISH_ERROR",
                "Failed to publish BatchQualityFailed event: " + e.getMessage(),
                "event_publishing"
            ));
        }
    }
    
    @Override
    public CompletableFuture<Result<Void>> publishBatchQualityFailedAsync(BatchQualityFailedEvent event) {
        return CompletableFuture.supplyAsync(() -> publishBatchQualityFailed(event), eventExecutor);
    }
    
    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<Void> publishQualityAlert(QualityAlertEvent event) {
        logger.info("Publishing QualityAlert event for batch: {} with alert type: {}", 
            event.getBatchId().getValue(), event.getAlertType());
        
        try {
            // Create the event object
            QualityAlertEventImpl eventImpl = new QualityAlertEventImpl(
                event.getBatchId(),
                event.getBankId(),
                event.getAlertType(),
                event.getAlertMessage(),
                event.getQualityScores(),
                event.getAlertMetadata()
            );
            
            // Publish the event
            eventPublisher.publishEvent(eventImpl);
            
            logger.info("Successfully published QualityAlert event for batch: {}", 
                event.getBatchId().getValue());
            
            return Result.success();
            
        } catch (Exception e) {
            logger.error("Failed to publish QualityAlert event for batch: {}", 
                event.getBatchId().getValue(), e);
            
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISH_ERROR",
                "Failed to publish QualityAlert event: " + e.getMessage(),
                "event_publishing"
            ));
        }
    }
    
    /**
     * Publish multiple events in batch (for efficiency).
     */
    public CompletableFuture<Result<Void>> publishEventsAsync(java.util.List<Object> events) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (Object event : events) {
                    eventPublisher.publishEvent(event);
                }
                
                logger.info("Successfully published {} events in batch", events.size());
                return Result.success();
                
            } catch (Exception e) {
                logger.error("Failed to publish events in batch", e);
                return Result.failure(ErrorDetail.of(
                    "BATCH_EVENT_PUBLISH_ERROR",
                    "Failed to publish events in batch: " + e.getMessage(),
                    "event_publishing"
                ));
            }
        }, eventExecutor);
    }
    
    /**
     * Shutdown the event executor.
     */
    public void shutdown() {
        eventExecutor.shutdown();
    }
}

/**
 * Implementation of BatchQualityCompletedEvent for Spring event publishing.
 */
class BatchQualityCompletedEventImpl {
    private final BatchId batchId;
    private final BankId bankId;
    private final QualityScores qualityScores;
    private final S3Reference s3Reference;
    private final Map<String, Object> validationSummary;
    private final Map<String, Object> processingMetadata;
    private final Instant timestamp;
    
    public BatchQualityCompletedEventImpl(
        BatchId batchId,
        BankId bankId,
        QualityScores qualityScores,
        S3Reference s3Reference,
        Map<String, Object> validationSummary,
        Map<String, Object> processingMetadata
    ) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.qualityScores = qualityScores;
        this.s3Reference = s3Reference;
        this.validationSummary = validationSummary != null ? validationSummary : new HashMap<>();
        this.processingMetadata = processingMetadata != null ? processingMetadata : new HashMap<>();
        this.timestamp = Instant.now();
    }
    
    // Getters
    public BatchId getBatchId() { return batchId; }
    public BankId getBankId() { return bankId; }
    public QualityScores getQualityScores() { return qualityScores; }
    public S3Reference getS3Reference() { return s3Reference; }
    public Map<String, Object> getValidationSummary() { return validationSummary; }
    public Map<String, Object> getProcessingMetadata() { return processingMetadata; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "BatchQualityCompletedEvent{" +
                "batchId=" + batchId.getValue() +
                ", bankId=" + bankId.getValue() +
                ", overallScore=" + qualityScores.overallScore() +
                ", grade=" + qualityScores.grade() +
                ", timestamp=" + timestamp +
                '}';
    }
}

/**
 * Implementation of BatchQualityFailedEvent for Spring event publishing.
 */
class BatchQualityFailedEventImpl {
    private final BatchId batchId;
    private final BankId bankId;
    private final String errorMessage;
    private final Map<String, Object> errorDetails;
    private final Map<String, Object> processingMetadata;
    private final Instant timestamp;
    
    public BatchQualityFailedEventImpl(
        BatchId batchId,
        BankId bankId,
        String errorMessage,
        Map<String, Object> errorDetails,
        Map<String, Object> processingMetadata
    ) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails != null ? errorDetails : new HashMap<>();
        this.processingMetadata = processingMetadata != null ? processingMetadata : new HashMap<>();
        this.timestamp = Instant.now();
    }
    
    // Getters
    public BatchId getBatchId() { return batchId; }
    public BankId getBankId() { return bankId; }
    public String getErrorMessage() { return errorMessage; }
    public Map<String, Object> getErrorDetails() { return errorDetails; }
    public Map<String, Object> getProcessingMetadata() { return processingMetadata; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "BatchQualityFailedEvent{" +
                "batchId=" + batchId.getValue() +
                ", bankId=" + bankId.getValue() +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

/**
 * Implementation of QualityAlertEvent for Spring event publishing.
 */
class QualityAlertEventImpl {
    private final BatchId batchId;
    private final BankId bankId;
    private final String alertType;
    private final String alertMessage;
    private final QualityScores qualityScores;
    private final Map<String, Object> alertMetadata;
    private final Instant timestamp;
    
    public QualityAlertEventImpl(
        BatchId batchId,
        BankId bankId,
        String alertType,
        String alertMessage,
        QualityScores qualityScores,
        Map<String, Object> alertMetadata
    ) {
        this.batchId = batchId;
        this.bankId = bankId;
        this.alertType = alertType;
        this.alertMessage = alertMessage;
        this.qualityScores = qualityScores;
        this.alertMetadata = alertMetadata != null ? alertMetadata : new HashMap<>();
        this.timestamp = Instant.now();
    }
    
    // Getters
    public BatchId getBatchId() { return batchId; }
    public BankId getBankId() { return bankId; }
    public String getAlertType() { return alertType; }
    public String getAlertMessage() { return alertMessage; }
    public QualityScores getQualityScores() { return qualityScores; }
    public Map<String, Object> getAlertMetadata() { return alertMetadata; }
    public Instant getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return "QualityAlertEvent{" +
                "batchId=" + batchId.getValue() +
                ", bankId=" + bankId.getValue() +
                ", alertType='" + alertType + '\'' +
                ", alertMessage='" + alertMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}