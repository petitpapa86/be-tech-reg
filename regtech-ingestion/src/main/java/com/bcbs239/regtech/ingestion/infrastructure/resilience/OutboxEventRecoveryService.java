package com.bcbs239.regtech.ingestion.infrastructure.resilience;

import com.bcbs239.regtech.core.infrastructure.outbox.OutboxMessage;
import com.bcbs239.regtech.core.infrastructure.outbox.OutboxMessageRepository;
import com.bcbs239.regtech.core.infrastructure.outbox.OutboxMessageStatus;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling outbox event delivery failures and recovery.
 * Provides mechanisms to retry failed events and handle stuck outbox messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventRecoveryService {
    
    private final OutboxMessageRepository outboxRepository;
    
    @Value("${regtech.outbox.max-retry-attempts:5}")
    private int maxRetryAttempts;
    
    @Value("${regtech.outbox.retry-delay-minutes:5}")
    private long retryDelayMinutes;
    
    @Value("${regtech.outbox.stuck-message-timeout-hours:2}")
    private long stuckMessageTimeoutHours;
    
    @Value("${regtech.outbox.dead-letter-timeout-days:7}")
    private long deadLetterTimeoutDays;
    
    /**
     * Handles outbox event delivery failures gracefully with exponential backoff retry.
     */
    @Transactional
    public Result<Void> handleEventDeliveryFailure(Long outboxMessageId, Exception deliveryException) {
        log.warn("Handling event delivery failure for outbox message ID: {}", outboxMessageId);
        
        try {
            OutboxMessage message = outboxRepository.findById(outboxMessageId)
                    .orElseThrow(() -> new IllegalArgumentException("Outbox message not found: " + outboxMessageId));
            
            int currentRetryCount = message.getRetryCount();
            
            if (currentRetryCount >= maxRetryAttempts) {
                log.error("Maximum retry attempts ({}) exceeded for outbox message {}, moving to dead letter", 
                         maxRetryAttempts, outboxMessageId);
                
                return moveToDeadLetter(message, deliveryException);
            }
            
            // Increment retry count and schedule for retry
            message.setRetryCount(currentRetryCount + 1);
            message.setStatus(OutboxMessageStatus.PENDING);
            message.setLastError(deliveryException.getMessage());
            
            // Calculate next retry time with exponential backoff
            long delayMinutes = calculateRetryDelay(currentRetryCount + 1);
            Instant nextRetryTime = Instant.now().plus(delayMinutes, ChronoUnit.MINUTES);
            message.setNextRetryTime(nextRetryTime);
            
            outboxRepository.save(message);
            
            log.info("Scheduled outbox message {} for retry attempt {} in {} minutes", 
                    outboxMessageId, currentRetryCount + 1, delayMinutes);
            
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to handle event delivery failure for outbox message {}: {}", 
                     outboxMessageId, e.getMessage());
            return Result.failure(ErrorDetail.of("EVENT_DELIVERY_FAILURE_HANDLING_ERROR", 
                "Failed to handle event delivery failure: " + e.getMessage()));
        }
    }
    
    /**
     * Moves a failed outbox message to dead letter status after exhausting retries.
     */
    private Result<Void> moveToDeadLetter(OutboxMessage message, Exception lastException) {
        try {
            message.setStatus(OutboxMessageStatus.DEAD_LETTER);
            message.setLastError(String.format("Moved to dead letter after %d failed attempts. Last error: %s", 
                                              message.getRetryCount(), lastException.getMessage()));
            message.setDeadLetterTime(Instant.now());
            
            outboxRepository.save(message);
            
            log.error("Moved outbox message {} to dead letter status after {} failed attempts", 
                     message.getId(), message.getRetryCount());
            
            // Could trigger alerting here for dead letter messages
            triggerDeadLetterAlert(message);
            
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to move outbox message {} to dead letter: {}", message.getId(), e.getMessage());
            return Result.failure(ErrorDetail.of("DEAD_LETTER_MOVE_FAILED", 
                "Failed to move message to dead letter: " + e.getMessage()));
        }
    }
    
    /**
     * Calculates retry delay with exponential backoff.
     */
    private long calculateRetryDelay(int retryAttempt) {
        // Exponential backoff: base delay * 2^(attempt-1)
        // With maximum cap to prevent extremely long delays
        long baseDelayMinutes = retryDelayMinutes;
        long exponentialDelay = baseDelayMinutes * (1L << Math.min(retryAttempt - 1, 6)); // Cap at 2^6
        
        // Add some jitter to prevent thundering herd
        long jitter = (long) (exponentialDelay * 0.1 * Math.random());
        
        return Math.min(exponentialDelay + jitter, 60); // Cap at 1 hour
    }
    
    /**
     * Scheduled task to process pending outbox messages that are ready for retry.
     */
    @Scheduled(fixedDelayString = "${regtech.outbox.retry-processor-interval-ms:30000}")
    @Async
    public CompletableFuture<Void> processRetryableOutboxMessages() {
        log.debug("Processing retryable outbox messages");
        
        try {
            Instant now = Instant.now();
            List<OutboxMessage> retryableMessages = outboxRepository.findRetryableMessages(now);
            
            if (retryableMessages.isEmpty()) {
                log.debug("No retryable outbox messages found");
                return CompletableFuture.completedFuture(null);
            }
            
            log.info("Found {} retryable outbox messages", retryableMessages.size());
            
            for (OutboxMessage message : retryableMessages) {
                try {
                    processRetryableMessage(message);
                } catch (Exception e) {
                    log.error("Error processing retryable message {}: {}", message.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Error in retry processor: {}", e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Processes a single retryable outbox message.
     */
    @Transactional
    private void processRetryableMessage(OutboxMessage message) {
        log.debug("Processing retryable outbox message: {} (attempt {})", 
                 message.getId(), message.getRetryCount());
        
        try {
            // Reset status to PENDING for processing
            message.setStatus(OutboxMessageStatus.PENDING);
            message.setProcessedAt(null);
            outboxRepository.save(message);
            
            log.info("Reset outbox message {} to PENDING for retry processing", message.getId());
            
        } catch (Exception e) {
            log.error("Failed to reset retryable message {} to PENDING: {}", message.getId(), e.getMessage());
        }
    }
    
    /**
     * Scheduled task to identify and handle stuck outbox messages.
     */
    @Scheduled(fixedDelayString = "${regtech.outbox.stuck-message-processor-interval-ms:300000}")
    @Async
    public CompletableFuture<Result<List<Long>>> processStuckOutboxMessages() {
        log.debug("Processing stuck outbox messages");
        
        try {
            Instant cutoffTime = Instant.now().minus(stuckMessageTimeoutHours, ChronoUnit.HOURS);
            List<OutboxMessage> stuckMessages = outboxRepository.findStuckMessages(cutoffTime);
            
            if (stuckMessages.isEmpty()) {
                log.debug("No stuck outbox messages found");
                return CompletableFuture.completedFuture(Result.success(new ArrayList<>()));
            }
            
            log.warn("Found {} stuck outbox messages", stuckMessages.size());
            
            List<Long> recoveredMessageIds = new ArrayList<>();
            List<ErrorDetail> errors = new ArrayList<>();
            
            for (OutboxMessage message : stuckMessages) {
                try {
                    Result<Void> recoveryResult = recoverStuckMessage(message);
                    
                    if (recoveryResult.isSuccess()) {
                        recoveredMessageIds.add(message.getId());
                        log.info("Successfully recovered stuck outbox message: {}", message.getId());
                    } else {
                        log.error("Failed to recover stuck outbox message {}: {}", 
                                 message.getId(), 
                                 recoveryResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                        errors.addAll(recoveryResult.getErrors());
                    }
                    
                } catch (Exception e) {
                    log.error("Exception during recovery of stuck outbox message {}: {}", 
                             message.getId(), e.getMessage());
                    errors.add(ErrorDetail.of("STUCK_MESSAGE_RECOVERY_ERROR", 
                        String.format("Failed to recover stuck message %d: %s", message.getId(), e.getMessage())));
                }
            }
            
            log.info("Stuck message processing completed: {} recovered, {} errors", 
                    recoveredMessageIds.size(), errors.size());
            
            if (errors.isEmpty()) {
                return CompletableFuture.completedFuture(Result.success(recoveredMessageIds));
            } else {
                return CompletableFuture.completedFuture(Result.failure(errors));
            }
            
        } catch (Exception e) {
            log.error("Error during stuck message processing: {}", e.getMessage());
            return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of("STUCK_MESSAGE_PROCESSING_ERROR", 
                "Error during stuck message processing: " + e.getMessage())));
        }
    }
    
    /**
     * Recovers a stuck outbox message by resetting its status.
     */
    @Transactional
    private Result<Void> recoverStuckMessage(OutboxMessage message) {
        log.info("Recovering stuck outbox message: {} (status: {}, last updated: {})", 
                message.getId(), message.getStatus(), message.getUpdatedAt());
        
        try {
            // Reset the message for retry
            message.setStatus(OutboxMessageStatus.PENDING);
            message.setProcessedAt(null);
            message.setLastError("Recovered from stuck state");
            
            // Don't increment retry count for stuck messages - they might have been stuck due to system issues
            
            outboxRepository.save(message);
            
            log.info("Successfully recovered stuck outbox message: {}", message.getId());
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to recover stuck outbox message {}: {}", message.getId(), e.getMessage());
            return Result.failure(ErrorDetail.of("STUCK_MESSAGE_RECOVERY_FAILED", 
                "Failed to recover stuck message: " + e.getMessage()));
        }
    }
    
    /**
     * Scheduled task to clean up old dead letter messages.
     */
    @Scheduled(fixedDelayString = "${regtech.outbox.dead-letter-cleanup-interval-ms:86400000}") // Daily
    @Async
    public CompletableFuture<Result<Integer>> cleanupOldDeadLetterMessages() {
        log.info("Starting cleanup of old dead letter messages");
        
        try {
            Instant cutoffTime = Instant.now().minus(deadLetterTimeoutDays, ChronoUnit.DAYS);
            List<OutboxMessage> oldDeadLetterMessages = outboxRepository.findOldDeadLetterMessages(cutoffTime);
            
            if (oldDeadLetterMessages.isEmpty()) {
                log.info("No old dead letter messages found for cleanup");
                return CompletableFuture.completedFuture(Result.success(0));
            }
            
            log.info("Found {} old dead letter messages for cleanup", oldDeadLetterMessages.size());
            
            int cleanedCount = 0;
            for (OutboxMessage message : oldDeadLetterMessages) {
                try {
                    // Archive or delete the message
                    outboxRepository.delete(message);
                    cleanedCount++;
                    
                    log.debug("Cleaned up old dead letter message: {}", message.getId());
                    
                } catch (Exception e) {
                    log.error("Failed to cleanup dead letter message {}: {}", message.getId(), e.getMessage());
                }
            }
            
            log.info("Cleaned up {} old dead letter messages", cleanedCount);
            return CompletableFuture.completedFuture(Result.success(cleanedCount));
            
        } catch (Exception e) {
            log.error("Error during dead letter cleanup: {}", e.getMessage());
            return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of("DEAD_LETTER_CLEANUP_ERROR", 
                "Error during dead letter cleanup: " + e.getMessage())));
        }
    }
    
    /**
     * Gets outbox health status for monitoring.
     */
    public Result<OutboxHealthStatus> getOutboxHealthStatus() {
        try {
            long pendingCount = outboxRepository.countByStatus(OutboxMessageStatus.PENDING);
            long publishedCount = outboxRepository.countByStatus(OutboxMessageStatus.PUBLISHED);
            long deadLetterCount = outboxRepository.countByStatus(OutboxMessageStatus.DEAD_LETTER);
            
            Instant cutoffTime = Instant.now().minus(stuckMessageTimeoutHours, ChronoUnit.HOURS);
            long stuckCount = outboxRepository.countStuckMessages(cutoffTime);
            
            OutboxHealthStatus status = new OutboxHealthStatus(
                pendingCount,
                publishedCount,
                deadLetterCount,
                stuckCount,
                System.currentTimeMillis()
            );
            
            return Result.success(status);
            
        } catch (Exception e) {
            log.error("Failed to get outbox health status: {}", e.getMessage());
            return Result.failure(ErrorDetail.of("OUTBOX_HEALTH_STATUS_ERROR", 
                "Failed to get outbox health status: " + e.getMessage()));
        }
    }
    
    /**
     * Triggers an alert for dead letter messages (placeholder for actual alerting implementation).
     */
    private void triggerDeadLetterAlert(OutboxMessage message) {
        // This would integrate with your alerting system (e.g., send to monitoring, email, Slack, etc.)
        log.error("ALERT: Outbox message {} moved to dead letter - manual intervention required. " +
                 "Event type: {}, Payload: {}", 
                 message.getId(), message.getEventType(), message.getEventPayload());
    }
    
    /**
     * Outbox health status for monitoring.
     */
    public record OutboxHealthStatus(
        long pendingCount,
        long publishedCount,
        long deadLetterCount,
        long stuckCount,
        long checkTimestamp
    ) {
        
        public boolean isHealthy() {
            return deadLetterCount == 0 && stuckCount == 0;
        }
        
        public String getHealthSummary() {
            if (isHealthy()) {
                return "HEALTHY";
            } else if (deadLetterCount > 0) {
                return "UNHEALTHY - Dead letter messages present";
            } else if (stuckCount > 0) {
                return "DEGRADED - Stuck messages detected";
            } else {
                return "UNKNOWN";
            }
        }
        
        public double getSuccessRate() {
            long totalProcessed = publishedCount + deadLetterCount;
            if (totalProcessed == 0) {
                return 1.0; // No messages processed yet
            }
            return (double) publishedCount / totalProcessed;
        }
    }
}