package com.bcbs239.regtech.ingestion.application.batch.queries;

import com.bcbs239.regtech.ingestion.domain.batch.BatchStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object for batch status information.
 * Contains current status, progress, timing, and performance metrics.
 */
@Data
@Builder
public class BatchStatusDto {
    
    private final String batchId;
    private final String bankId;
    private final BatchStatus status;
    private final String processingStage;
    private final int progressPercentage;
    private final Instant uploadedAt;
    private final Instant completedAt;
    private final Long processingDurationMs;
    private final Long estimatedCompletionTimeMs;
    private final String fileName;
    private final String contentType;
    private final Long fileSizeBytes;
    private final Integer totalExposures;
    private final String s3Uri;
    private final String errorMessage;
    private final Map<String, Object> performanceMetrics;
    private final Map<String, String> downloadLinks;
    
    /**
     * Check if the batch processing is complete.
     */
    public boolean isCompleted() {
        return status == BatchStatus.COMPLETED;
    }
    
    /**
     * Check if the batch processing has failed.
     */
    public boolean isFailed() {
        return status == BatchStatus.FAILED;
    }
    
    /**
     * Check if the batch is currently being processed.
     */
    public boolean isInProgress() {
        return status == BatchStatus.PARSING || 
               status == BatchStatus.VALIDATED || 
               status == BatchStatus.STORING;
    }
    
    /**
     * Get human-readable status description.
     */
    public String getStatusDescription() {
        return switch (status) {
            case UPLOADED -> "File uploaded and queued for processing";
            case PARSING -> "Parsing and validating file content";
            case VALIDATED -> "File validated, enriching with bank information";
            case STORING -> "Storing file in secure storage";
            case COMPLETED -> "Processing completed successfully";
            case FAILED -> "Processing failed: " + (errorMessage != null ? errorMessage : "Unknown error");
        };
    }
    
    /**
     * Get estimated time remaining in milliseconds.
     */
    public Long getEstimatedTimeRemainingMs() {
        if (estimatedCompletionTimeMs == null || isCompleted() || isFailed()) {
            return null;
        }
        
        long remaining = estimatedCompletionTimeMs - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
}

