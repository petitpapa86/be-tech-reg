package com.bcbs239.regtech.ingestion.infrastructure.resilience;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.model.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.repository.IngestionBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for system recovery mechanisms including checkpoint-based recovery
 * and handling of stuck or failed batch processing operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemRecoveryService {
    
    private final IngestionBatchRepository batchRepository;
    private final DatabaseTransactionManager transactionManager;
    
    @Value("${regtech.recovery.stuck-batch-timeout-minutes:30}")
    private long stuckBatchTimeoutMinutes;
    
    @Value("${regtech.recovery.max-recovery-attempts:3}")
    private int maxRecoveryAttempts;
    
    @Value("${regtech.recovery.checkpoint-interval-minutes:5}")
    private long checkpointIntervalMinutes;
    
    /**
     * Resumes processing from the last successful checkpoint for a given batch.
     */
    @Transactional
    public Result<Void> resumeProcessingFromCheckpoint(BatchId batchId) {
        log.info("Attempting to resume processing from checkpoint for batch: {}", batchId.value());
        
        Optional<IngestionBatch> batchOpt = batchRepository.findByBatchId(batchId);
        if (batchOpt.isEmpty()) {
            return Result.failure(ErrorDetail.of("BATCH_NOT_FOUND", 
                "Cannot resume processing: batch not found: " + batchId.value()));
        }
        
        IngestionBatch batch = batchOpt.get();
        BatchStatus currentStatus = batch.getStatus();
        
        log.info("Found batch {} with status {} for checkpoint recovery", batchId.value(), currentStatus);
        
        // Determine recovery strategy based on current status
        return switch (currentStatus) {
            case UPLOADED -> resumeFromUploadedState(batch);
            case PARSING -> resumeFromParsingState(batch);
            case VALIDATED -> resumeFromValidatedState(batch);
            case STORING -> resumeFromStoringState(batch);
            case COMPLETED -> {
                log.info("Batch {} is already completed, no recovery needed", batchId.value());
                yield Result.success(null);
            }
            case FAILED -> resumeFromFailedState(batch);
        };
    }
    
    /**
     * Resumes processing from UPLOADED state.
     */
    private Result<Void> resumeFromUploadedState(IngestionBatch batch) {
        log.info("Resuming batch {} from UPLOADED state", batch.getBatchId().value());
        
        try {
            // Reset any error state and restart parsing
            batch.clearErrorMessage();
            batch.updateStatus(BatchStatus.PARSING);
            batchRepository.save(batch);
            
            log.info("Successfully reset batch {} to PARSING state for recovery", batch.getBatchId().value());
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to resume batch {} from UPLOADED state: {}", batch.getBatchId().value(), e.getMessage());
            return Result.failure(ErrorDetail.of("RESUME_FROM_UPLOADED_FAILED", 
                "Failed to resume from UPLOADED state: " + e.getMessage()));
        }
    }
    
    /**
     * Resumes processing from PARSING state.
     */
    private Result<Void> resumeFromParsingState(IngestionBatch batch) {
        log.info("Resuming batch {} from PARSING state", batch.getBatchId().value());
        
        // Check if parsing was actually completed but status wasn't updated
        if (batch.getTotalExposures() != null && batch.getTotalExposures() > 0) {
            log.info("Batch {} appears to have completed parsing, advancing to VALIDATED", batch.getBatchId().value());
            
            try {
                batch.updateStatus(BatchStatus.VALIDATED);
                batchRepository.save(batch);
                return Result.success(null);
            } catch (Exception e) {
                log.error("Failed to advance batch {} to VALIDATED: {}", batch.getBatchId().value(), e.getMessage());
                return Result.failure(ErrorDetail.of("ADVANCE_TO_VALIDATED_FAILED", 
                    "Failed to advance to VALIDATED state: " + e.getMessage()));
            }
        }
        
        // Restart parsing from beginning
        try {
            batch.clearErrorMessage();
            batchRepository.save(batch);
            
            log.info("Successfully reset batch {} for parsing retry", batch.getBatchId().value());
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to resume batch {} from PARSING state: {}", batch.getBatchId().value(), e.getMessage());
            return Result.failure(ErrorDetail.of("RESUME_FROM_PARSING_FAILED", 
                "Failed to resume from PARSING state: " + e.getMessage()));
        }
    }
    
    /**
     * Resumes processing from VALIDATED state.
     */
    private Result<Void> resumeFromValidatedState(IngestionBatch batch) {
        log.info("Resuming batch {} from VALIDATED state", batch.getBatchId().value());
        
        try {
            // Check if bank info enrichment is needed
            if (batch.getBankInfo() == null) {
                log.info("Batch {} needs bank info enrichment before proceeding", batch.getBatchId().value());
                // Bank info enrichment would be triggered by the processing handler
            }
            
            // Advance to STORING state
            batch.updateStatus(BatchStatus.STORING);
            batch.clearErrorMessage();
            batchRepository.save(batch);
            
            log.info("Successfully advanced batch {} to STORING state for recovery", batch.getBatchId().value());
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to resume batch {} from VALIDATED state: {}", batch.getBatchId().value(), e.getMessage());
            return Result.failure(ErrorDetail.of("RESUME_FROM_VALIDATED_FAILED", 
                "Failed to resume from VALIDATED state: " + e.getMessage()));
        }
    }
    
    /**
     * Resumes processing from STORING state.
     */
    private Result<Void> resumeFromStoringState(IngestionBatch batch) {
        log.info("Resuming batch {} from STORING state", batch.getBatchId().value());
        
        try {
            // Check if S3 storage was actually completed
            if (batch.getS3Reference() != null) {
                log.info("Batch {} appears to have completed S3 storage, advancing to COMPLETED", batch.getBatchId().value());
                
                batch.updateStatus(BatchStatus.COMPLETED);
                batch.setCompletedAt(Instant.now());
                batchRepository.save(batch);
                
                return Result.success(null);
            }
            
            // Clear any error state and retry S3 storage
            batch.clearErrorMessage();
            batchRepository.save(batch);
            
            log.info("Successfully reset batch {} for S3 storage retry", batch.getBatchId().value());
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to resume batch {} from STORING state: {}", batch.getBatchId().value(), e.getMessage());
            return Result.failure(ErrorDetail.of("RESUME_FROM_STORING_FAILED", 
                "Failed to resume from STORING state: " + e.getMessage()));
        }
    }
    
    /**
     * Attempts to recover from FAILED state by analyzing the failure and determining if recovery is possible.
     */
    private Result<Void> resumeFromFailedState(IngestionBatch batch) {
        log.info("Attempting to recover batch {} from FAILED state", batch.getBatchId().value());
        
        String errorMessage = batch.getErrorMessage();
        if (errorMessage == null) {
            errorMessage = "Unknown error";
        }
        
        // Analyze if the failure is recoverable
        if (isRecoverableFailure(errorMessage)) {
            log.info("Failure appears recoverable for batch {}, attempting recovery", batch.getBatchId().value());
            
            try {
                // Increment recovery attempt count
                int currentAttempts = batch.getRecoveryAttempts();
                if (currentAttempts >= maxRecoveryAttempts) {
                    log.error("Maximum recovery attempts ({}) exceeded for batch {}", 
                             maxRecoveryAttempts, batch.getBatchId().value());
                    return Result.failure(ErrorDetail.of("MAX_RECOVERY_ATTEMPTS_EXCEEDED", 
                        "Maximum recovery attempts exceeded for batch: " + batch.getBatchId().value()));
                }
                
                batch.incrementRecoveryAttempts();
                batch.clearErrorMessage();
                
                // Reset to appropriate state based on what was completed
                BatchStatus resetStatus = determineResetStatus(batch);
                batch.updateStatus(resetStatus);
                
                batchRepository.save(batch);
                
                log.info("Successfully reset failed batch {} to {} state for recovery attempt {}", 
                        batch.getBatchId().value(), resetStatus, batch.getRecoveryAttempts());
                
                return Result.success(null);
                
            } catch (Exception e) {
                log.error("Failed to recover batch {} from FAILED state: {}", batch.getBatchId().value(), e.getMessage());
                return Result.failure(ErrorDetail.of("RECOVERY_FROM_FAILED_ERROR", 
                    "Failed to recover from FAILED state: " + e.getMessage()));
            }
        } else {
            log.error("Failure is not recoverable for batch {}: {}", batch.getBatchId().value(), errorMessage);
            return Result.failure(ErrorDetail.of("NON_RECOVERABLE_FAILURE", 
                "Batch failure is not recoverable: " + errorMessage));
        }
    }
    
    /**
     * Determines if a failure is recoverable based on the error message.
     */
    private boolean isRecoverableFailure(String errorMessage) {
        String lowerMessage = errorMessage.toLowerCase();
        
        // Non-recoverable failures
        if (lowerMessage.contains("validation") ||
            lowerMessage.contains("invalid format") ||
            lowerMessage.contains("parse error") ||
            lowerMessage.contains("checksum mismatch") ||
            lowerMessage.contains("authentication") ||
            lowerMessage.contains("authorization") ||
            lowerMessage.contains("access denied")) {
            return false;
        }
        
        // Recoverable failures
        return lowerMessage.contains("timeout") ||
               lowerMessage.contains("connection") ||
               lowerMessage.contains("service unavailable") ||
               lowerMessage.contains("temporary") ||
               lowerMessage.contains("network") ||
               lowerMessage.contains("throttling");
    }
    
    /**
     * Determines the appropriate status to reset a failed batch to based on what was completed.
     */
    private BatchStatus determineResetStatus(IngestionBatch batch) {
        // Check what was successfully completed
        if (batch.getS3Reference() != null) {
            return BatchStatus.STORING; // S3 storage was in progress
        } else if (batch.getBankInfo() != null) {
            return BatchStatus.VALIDATED; // Bank enrichment was completed
        } else if (batch.getTotalExposures() != null && batch.getTotalExposures() > 0) {
            return BatchStatus.VALIDATED; // Parsing was completed
        } else {
            return BatchStatus.UPLOADED; // Start from the beginning
        }
    }
    
    /**
     * Finds and recovers stuck batches that have been in processing states for too long.
     */
    @Async
    public CompletableFuture<Result<List<BatchId>>> recoverStuckBatches() {
        log.info("Starting stuck batch recovery process");
        
        try {
            Instant cutoffTime = Instant.now().minus(stuckBatchTimeoutMinutes, ChronoUnit.MINUTES);
            
            List<IngestionBatch> stuckBatches = batchRepository.findStuckBatches(
                List.of(BatchStatus.PARSING, BatchStatus.VALIDATED, BatchStatus.STORING),
                cutoffTime
            );
            
            log.info("Found {} stuck batches for recovery", stuckBatches.size());
            
            List<BatchId> recoveredBatches = new ArrayList<>();
            List<ErrorDetail> recoveryErrors = new ArrayList<>();
            
            for (IngestionBatch batch : stuckBatches) {
                try {
                    log.info("Attempting to recover stuck batch: {} (status: {}, last updated: {})", 
                            batch.getBatchId().value(), batch.getStatus(), batch.getUpdatedAt());
                    
                    Result<Void> recoveryResult = resumeProcessingFromCheckpoint(batch.getBatchId());
                    
                    if (recoveryResult.isSuccess()) {
                        recoveredBatches.add(batch.getBatchId());
                        log.info("Successfully recovered stuck batch: {}", batch.getBatchId().value());
                    } else {
                        log.error("Failed to recover stuck batch {}: {}", 
                                 batch.getBatchId().value(), 
                                 recoveryResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error"));
                        recoveryErrors.addAll(recoveryResult.getErrors());
                    }
                    
                } catch (Exception e) {
                    log.error("Exception during recovery of stuck batch {}: {}", 
                             batch.getBatchId().value(), e.getMessage());
                    recoveryErrors.add(ErrorDetail.of("STUCK_BATCH_RECOVERY_ERROR", 
                        String.format("Failed to recover stuck batch %s: %s", 
                                     batch.getBatchId().value(), e.getMessage())));
                }
            }
            
            log.info("Stuck batch recovery completed: {} recovered, {} errors", 
                    recoveredBatches.size(), recoveryErrors.size());
            
            if (recoveryErrors.isEmpty()) {
                return CompletableFuture.completedFuture(Result.success(recoveredBatches));
            } else {
                return CompletableFuture.completedFuture(Result.failure(recoveryErrors));
            }
            
        } catch (Exception e) {
            log.error("Error during stuck batch recovery process: {}", e.getMessage());
            return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of("STUCK_BATCH_RECOVERY_PROCESS_ERROR", 
                "Error during stuck batch recovery process: " + e.getMessage())));
        }
    }
    
    /**
     * Creates a recovery checkpoint for a batch at a specific processing stage.
     */
    @Transactional
    public Result<Void> createRecoveryCheckpoint(BatchId batchId, BatchStatus status, String checkpointData) {
        log.debug("Creating recovery checkpoint for batch {} at status {}", batchId.value(), status);
        
        try {
            Optional<IngestionBatch> batchOpt = batchRepository.findByBatchId(batchId);
            if (batchOpt.isEmpty()) {
                return Result.failure(ErrorDetail.of("BATCH_NOT_FOUND", 
                    "Cannot create checkpoint: batch not found: " + batchId.value()));
            }
            
            IngestionBatch batch = batchOpt.get();
            batch.updateStatus(status);
            batch.setLastCheckpoint(Instant.now());
            
            // Store checkpoint data if provided
            if (checkpointData != null && !checkpointData.trim().isEmpty()) {
                batch.setCheckpointData(checkpointData);
            }
            
            batchRepository.save(batch);
            
            log.debug("Successfully created recovery checkpoint for batch {} at status {}", batchId.value(), status);
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to create recovery checkpoint for batch {}: {}", batchId.value(), e.getMessage());
            return Result.failure(ErrorDetail.of("CHECKPOINT_CREATION_FAILED", 
                "Failed to create recovery checkpoint: " + e.getMessage()));
        }
    }
    
    /**
     * Gets recovery status and recommendations for a specific batch.
     */
    public Result<RecoveryStatus> getRecoveryStatus(BatchId batchId) {
        log.debug("Getting recovery status for batch: {}", batchId.value());
        
        try {
            Optional<IngestionBatch> batchOpt = batchRepository.findByBatchId(batchId);
            if (batchOpt.isEmpty()) {
                return Result.failure(ErrorDetail.of("BATCH_NOT_FOUND", 
                    "Batch not found: " + batchId.value()));
            }
            
            IngestionBatch batch = batchOpt.get();
            
            boolean isStuck = isStuckBatch(batch);
            boolean isRecoverable = batch.getStatus() == BatchStatus.FAILED ? 
                isRecoverableFailure(batch.getErrorMessage()) : true;
            
            List<String> recommendations = generateRecoveryRecommendations(batch, isStuck);
            
            RecoveryStatus status = new RecoveryStatus(
                batch.getBatchId(),
                batch.getStatus(),
                isStuck,
                isRecoverable,
                batch.getRecoveryAttempts(),
                maxRecoveryAttempts,
                batch.getLastCheckpoint(),
                batch.getErrorMessage(),
                recommendations
            );
            
            return Result.success(status);
            
        } catch (Exception e) {
            log.error("Failed to get recovery status for batch {}: {}", batchId.value(), e.getMessage());
            return Result.failure(ErrorDetail.of("RECOVERY_STATUS_ERROR", 
                "Failed to get recovery status: " + e.getMessage()));
        }
    }
    
    /**
     * Determines if a batch is stuck based on its status and last update time.
     */
    private boolean isStuckBatch(IngestionBatch batch) {
        if (batch.getStatus() == BatchStatus.COMPLETED || batch.getStatus() == BatchStatus.FAILED) {
            return false;
        }
        
        Instant cutoffTime = Instant.now().minus(stuckBatchTimeoutMinutes, ChronoUnit.MINUTES);
        return batch.getUpdatedAt().isBefore(cutoffTime);
    }
    
    /**
     * Generates recovery recommendations based on batch state.
     */
    private List<String> generateRecoveryRecommendations(IngestionBatch batch, boolean isStuck) {
        List<String> recommendations = new ArrayList<>();
        
        if (isStuck) {
            recommendations.add("Batch appears to be stuck - consider manual recovery");
            recommendations.add("Check system logs for processing errors");
        }
        
        if (batch.getStatus() == BatchStatus.FAILED) {
            if (isRecoverableFailure(batch.getErrorMessage())) {
                recommendations.add("Failure appears recoverable - retry processing");
                recommendations.add("Check external service availability");
            } else {
                recommendations.add("Failure is not recoverable - manual intervention required");
                recommendations.add("Review and correct input data");
            }
        }
        
        if (batch.getRecoveryAttempts() > 0) {
            recommendations.add(String.format("Recovery has been attempted %d times", batch.getRecoveryAttempts()));
            if (batch.getRecoveryAttempts() >= maxRecoveryAttempts) {
                recommendations.add("Maximum recovery attempts reached - manual intervention required");
            }
        }
        
        return recommendations;
    }
    
    /**
     * Recovery status information for monitoring and decision making.
     */
    public record RecoveryStatus(
        BatchId batchId,
        BatchStatus currentStatus,
        boolean isStuck,
        boolean isRecoverable,
        int recoveryAttempts,
        int maxRecoveryAttempts,
        Instant lastCheckpoint,
        String errorMessage,
        List<String> recommendations
    ) {
        
        public boolean canAttemptRecovery() {
            return isRecoverable && recoveryAttempts < maxRecoveryAttempts;
        }
        
        public boolean needsManualIntervention() {
            return !isRecoverable || recoveryAttempts >= maxRecoveryAttempts;
        }
    }
}