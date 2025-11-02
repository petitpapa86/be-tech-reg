package com.bcbs239.regtech.ingestion.infrastructure.compliance;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.infrastructure.persistence.IngestionBatchEntity;
import com.bcbs239.regtech.ingestion.infrastructure.persistence.IngestionBatchRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for automated data cleanup operations based on retention policies.
 * Handles deletion of expired files and cleanup of orphaned data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataCleanupService {
    
    private final S3Client s3Client;
    private final IngestionBatchRepositoryImpl batchRepository;
    private final DataRetentionService dataRetentionService;
    
    @Value("${regtech.s3.bucket:regtech-data-storage}")
    private String bucketName;
    
    @Value("${regtech.compliance.cleanup.dry-run:true}")
    private boolean dryRun;
    
    @Value("${regtech.compliance.cleanup.batch-size:100}")
    private int cleanupBatchSize;
    
    /**
     * Performs automated cleanup of expired files based on retention policies.
     * This method should be called by scheduled tasks.
     */
    public Result<CleanupSummary> performAutomatedCleanup() {
        log.info("Starting automated data cleanup (dry-run: {})", dryRun);
        
        try {
            CleanupSummary.CleanupSummaryBuilder summaryBuilder = CleanupSummary.builder()
                    .cleanupStarted(Instant.now())
                    .dryRun(dryRun);
            
            // Find files eligible for deletion
            Instant reportEnd = Instant.now();
            Instant reportStart = reportEnd.minus(365 * 10, ChronoUnit.DAYS); // Look back 10 years
            
            List<IngestionBatchEntity> allBatches = batchRepository.findBatchEntitiesInPeriod(
                    reportStart, reportEnd);
            
            int filesEligibleForDeletion = 0;
            int filesDeleted = 0;
            int deletionErrors = 0;
            long totalBytesFreed = 0;
            
            for (IngestionBatchEntity batch : allBatches) {
                if (isEligibleForDeletion(batch)) {
                    filesEligibleForDeletion++;
                    
                    if (!dryRun) {
                        Result<Void> deleteResult = deleteFileFromS3(batch);
                        if (deleteResult.isSuccess()) {
                            filesDeleted++;
                            totalBytesFreed += batch.getFileSizeBytes() != null ? batch.getFileSizeBytes() : 0L;
                            log.info("Deleted expired file: {} (batch: {})", batch.getS3Uri(), batch.getBatchId());
                        } else {
                            deletionErrors++;
                            log.error("Failed to delete expired file: {} (batch: {})", 
                                    batch.getS3Uri(), batch.getBatchId());
                        }
                    } else {
                        log.info("Would delete expired file: {} (batch: {})", batch.getS3Uri(), batch.getBatchId());
                    }
                    
                    // Process in batches to avoid overwhelming the system
                    if (filesEligibleForDeletion % cleanupBatchSize == 0) {
                        log.info("Processed {} files for cleanup", filesEligibleForDeletion);
                        
                        // Add a small delay between batches
                        try {
                            Thread.sleep(1000); // 1 second delay
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            CleanupSummary summary = summaryBuilder
                    .cleanupCompleted(Instant.now())
                    .totalFilesScanned(allBatches.size())
                    .filesEligibleForDeletion(filesEligibleForDeletion)
                    .filesDeleted(filesDeleted)
                    .deletionErrors(deletionErrors)
                    .totalBytesFreed(totalBytesFreed)
                    .build();
            
            log.info("Automated cleanup completed: {}", summary.getSummary());
            return Result.success(summary);
            
        } catch (Exception e) {
            log.error("Failed to perform automated cleanup", e);
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of("CLEANUP_FAILED", 
                "Automated cleanup failed: " + e.getMessage()));
        }
    }
    
    /**
     * Deletes a file from S3 based on the batch information.
     */
    private Result<Void> deleteFileFromS3(IngestionBatchEntity batch) {
        try {
            if (batch.getS3Key() == null || batch.getS3Key().isEmpty()) {
                return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of("MISSING_S3_KEY", 
                    "Batch does not have S3 key information"));
            }
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(batch.getS3Key())
                    .versionId(batch.getS3VersionId()) // Delete specific version if available
                    .build();
            
            s3Client.deleteObject(deleteRequest);
            
            log.debug("Successfully deleted S3 object: {}", batch.getS3Key());
            return Result.success();
            
        } catch (S3Exception e) {
            log.error("Failed to delete S3 object {}: {}", batch.getS3Key(), e.getMessage());
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of("S3_DELETE_FAILED", 
                "Failed to delete S3 object: " + e.awsErrorDetails().errorMessage()));
        } catch (Exception e) {
            log.error("Unexpected error deleting S3 object {}: {}", batch.getS3Key(), e.getMessage());
            return Result.failure(com.bcbs239.regtech.core.shared.ErrorDetail.of("DELETE_ERROR", 
                "Unexpected error during deletion: " + e.getMessage()));
        }
    }
    
    /**
     * Determines if a batch is eligible for deletion based on retention policies.
     */
    private boolean isEligibleForDeletion(IngestionBatchEntity batch) {
        if (batch.getUploadedAt() == null) {
            return false;
        }
        
        // Get the retention policy for this batch
        String policyId = getPolicyForBatch(batch);
        DataRetentionPolicy policy = dataRetentionService.getRetentionPolicies().get(policyId);
        
        if (policy == null || policy.isHasLegalHoldRequirements()) {
            return false;
        }
        
        // Check if the file has exceeded its total retention period
        Instant expiryDate = batch.getUploadedAt().plus(policy.getTotalRetentionPeriod());
        boolean isExpired = Instant.now().isAfter(expiryDate);
        
        // Only delete if the policy allows early deletion and the file is expired
        return isExpired && policy.isAllowsEarlyDeletion();
    }
    
    /**
     * Determines the retention policy for a given batch.
     */
    private String getPolicyForBatch(IngestionBatchEntity batch) {
        // For now, use default policy for all batches
        // In a real implementation, this might be based on bank type, data type, etc.
        return "FINANCIAL_DATA_DEFAULT";
    }
    
    /**
     * Summary of cleanup operations.
     */
    public static record CleanupSummary(
            Instant cleanupStarted,
            Instant cleanupCompleted,
            boolean dryRun,
            int totalFilesScanned,
            int filesEligibleForDeletion,
            int filesDeleted,
            int deletionErrors,
            long totalBytesFreed
    ) {
        public static CleanupSummaryBuilder builder() {
            return new CleanupSummaryBuilder();
        }
        
        public String getSummary() {
            long durationMs = cleanupCompleted.toEpochMilli() - cleanupStarted.toEpochMilli();
            double durationSeconds = durationMs / 1000.0;
            
            return String.format(
                    "Cleanup %s: Scanned %d files, %d eligible for deletion, %d deleted, %d errors, %.2f MB freed (%.2fs)",
                    dryRun ? "(DRY RUN)" : "completed",
                    totalFilesScanned,
                    filesEligibleForDeletion,
                    filesDeleted,
                    deletionErrors,
                    totalBytesFreed / (1024.0 * 1024.0),
                    durationSeconds
            );
        }
        
        public static class CleanupSummaryBuilder {
            private Instant cleanupStarted;
            private Instant cleanupCompleted;
            private boolean dryRun;
            private int totalFilesScanned;
            private int filesEligibleForDeletion;
            private int filesDeleted;
            private int deletionErrors;
            private long totalBytesFreed;
            
            public CleanupSummaryBuilder cleanupStarted(Instant cleanupStarted) {
                this.cleanupStarted = cleanupStarted;
                return this;
            }
            
            public CleanupSummaryBuilder cleanupCompleted(Instant cleanupCompleted) {
                this.cleanupCompleted = cleanupCompleted;
                return this;
            }
            
            public CleanupSummaryBuilder dryRun(boolean dryRun) {
                this.dryRun = dryRun;
                return this;
            }
            
            public CleanupSummaryBuilder totalFilesScanned(int totalFilesScanned) {
                this.totalFilesScanned = totalFilesScanned;
                return this;
            }
            
            public CleanupSummaryBuilder filesEligibleForDeletion(int filesEligibleForDeletion) {
                this.filesEligibleForDeletion = filesEligibleForDeletion;
                return this;
            }
            
            public CleanupSummaryBuilder filesDeleted(int filesDeleted) {
                this.filesDeleted = filesDeleted;
                return this;
            }
            
            public CleanupSummaryBuilder deletionErrors(int deletionErrors) {
                this.deletionErrors = deletionErrors;
                return this;
            }
            
            public CleanupSummaryBuilder totalBytesFreed(long totalBytesFreed) {
                this.totalBytesFreed = totalBytesFreed;
                return this;
            }
            
            public CleanupSummary build() {
                return new CleanupSummary(
                        cleanupStarted,
                        cleanupCompleted,
                        dryRun,
                        totalFilesScanned,
                        filesEligibleForDeletion,
                        filesDeleted,
                        deletionErrors,
                        totalBytesFreed
                );
            }
        }
    }
}