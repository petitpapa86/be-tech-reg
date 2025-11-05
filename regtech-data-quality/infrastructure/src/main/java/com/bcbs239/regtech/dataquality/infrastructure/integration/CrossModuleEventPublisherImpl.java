package com.bcbs239.regtech.dataquality.infrastructure.integration;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.application.integration.CrossModuleEventPublisher;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
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
    public Result<Void> publishBatchQualityCompleted(BatchId batchId, BankId bankId, QualityScores qualityScores, S3Reference detailsReference) {
        logger.info("Publishing BatchQualityCompleted event for batch: {} with overall score: {}",
            batchId.value(), qualityScores.overallScore());

        try {
            // Build a minimal validation summary / metadata placeholders
            Map<String, Object> validationSummary = new HashMap<>();
            Map<String, Object> processingMetadata = new HashMap<>();

            BatchQualityCompletedEventImpl eventImpl = new BatchQualityCompletedEventImpl(
                batchId,
                bankId,
                qualityScores,
                detailsReference,
                validationSummary,
                processingMetadata
            );

            eventPublisher.publishEvent(eventImpl);

            logger.info("Successfully published BatchQualityCompleted event for batch: {}", batchId.value());
            return Result.success();

        } catch (Exception e) {
            logger.error("Failed to publish BatchQualityCompleted event for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISH_ERROR",
                "Failed to publish BatchQualityCompleted event: " + e.getMessage(),
                "event_publishing"
            ));
        }
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<Void> publishBatchQualityCompleted(BatchId batchId, BankId bankId, QualityScores qualityScores, S3Reference detailsReference, String correlationId) {
        // include correlationId in processing metadata when publishing
        logger.info("Publishing BatchQualityCompleted event for batch: {} with correlationId: {} and overall score: {}",
            batchId.value(), correlationId, qualityScores.overallScore());

        try {
            Map<String, Object> validationSummary = new HashMap<>();
            Map<String, Object> processingMetadata = new HashMap<>();
            if (correlationId != null) {
                processingMetadata.put("correlationId", correlationId);
            }

            BatchQualityCompletedEventImpl eventImpl = new BatchQualityCompletedEventImpl(
                batchId,
                bankId,
                qualityScores,
                detailsReference,
                validationSummary,
                processingMetadata
            );

            eventPublisher.publishEvent(eventImpl);

            logger.info("Successfully published BatchQualityCompleted event for batch: {}", batchId.value());
            return Result.success();

        } catch (Exception e) {
            logger.error("Failed to publish BatchQualityCompleted event for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISH_ERROR",
                "Failed to publish BatchQualityCompleted event: " + e.getMessage(),
                "event_publishing"
            ));
        }
    }

    public CompletableFuture<Result<Void>> publishBatchQualityCompletedAsync(BatchId batchId, BankId bankId, QualityScores qualityScores, S3Reference detailsReference) {
        return CompletableFuture.supplyAsync(() -> publishBatchQualityCompleted(batchId, bankId, qualityScores, detailsReference), eventExecutor);
    }
    
    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<Void> publishBatchQualityFailed(BatchId batchId, BankId bankId, String errorMessage) {
        logger.info("Publishing BatchQualityFailed event for batch: {} with error: {}", batchId.value(), errorMessage);

        try {
            Map<String, Object> errorDetails = new HashMap<>();
            Map<String, Object> processingMetadata = new HashMap<>();

            BatchQualityFailedEventImpl eventImpl = new BatchQualityFailedEventImpl(
                batchId,
                bankId,
                errorMessage,
                errorDetails,
                processingMetadata
            );

            eventPublisher.publishEvent(eventImpl);
            logger.info("Successfully published BatchQualityFailed event for batch: {}", batchId.value());
            return Result.success();

        } catch (Exception e) {
            logger.error("Failed to publish BatchQualityFailed event for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISH_ERROR",
                "Failed to publish BatchQualityFailed event: " + e.getMessage(),
                "event_publishing"
            ));
        }
    }

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public Result<Void> publishBatchQualityFailed(BatchId batchId, BankId bankId, String errorMessage, String correlationId) {
        logger.info("Publishing BatchQualityFailed event for batch: {} with error: {} and correlationId: {}", batchId.value(), errorMessage, correlationId);

        try {
            Map<String, Object> errorDetails = new HashMap<>();
            Map<String, Object> processingMetadata = new HashMap<>();
            if (correlationId != null) {
                processingMetadata.put("correlationId", correlationId);
            }

            BatchQualityFailedEventImpl eventImpl = new BatchQualityFailedEventImpl(
                batchId,
                bankId,
                errorMessage,
                errorDetails,
                processingMetadata
            );

            eventPublisher.publishEvent(eventImpl);
            logger.info("Successfully published BatchQualityFailed event for batch: {}", batchId.value());
            return Result.success();

        } catch (Exception e) {
            logger.error("Failed to publish BatchQualityFailed event for batch: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISH_ERROR",
                "Failed to publish BatchQualityFailed event: " + e.getMessage(),
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
                "batchId=" + batchId.value() +
                ", bankId=" + bankId.value() +
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
                "batchId=" + batchId.value() +
                ", bankId=" + bankId.value() +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

