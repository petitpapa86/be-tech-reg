package com.bcbs239.regtech.ingestion.application.query;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.ingestion.application.service.JwtTokenService;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.model.IngestionBatch;
import com.bcbs239.regtech.ingestion.domain.repository.IngestionBatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Query handler for retrieving batch status and processing information.
 * Handles authentication, authorization, and status calculation.
 */
@Component
public class BatchStatusQueryHandler {
    
    private final JwtTokenService jwtTokenService;
    private final IngestionBatchRepository ingestionBatchRepository;
    
    // Estimated processing times per stage (in milliseconds)
    private static final Map<BatchStatus, Long> ESTIMATED_STAGE_DURATIONS = Map.of(
        BatchStatus.PARSING, 30_000L,      // 30 seconds
        BatchStatus.VALIDATED, 15_000L,    // 15 seconds  
        BatchStatus.STORING, 60_000L       // 60 seconds
    );
    
    @Autowired
    public BatchStatusQueryHandler(
            JwtTokenService jwtTokenService,
            IngestionBatchRepository ingestionBatchRepository) {
        this.jwtTokenService = jwtTokenService;
        this.ingestionBatchRepository = ingestionBatchRepository;
    }
    
    /**
     * Handle the batch status query.
     * 1. Validate JWT token and extract bank ID
     * 2. Find batch by ID
     * 3. Verify bank authorization (batch belongs to requesting bank)
     * 4. Calculate progress and estimated completion time
     * 5. Return comprehensive status information
     */
    public Result<BatchStatusDto> handle(BatchStatusQuery query) {
        try {
            // 1. Validate JWT token and extract bank ID
            Result<Void> tokenValidation = jwtTokenService.validateToken(query.authToken());
            if (tokenValidation.isFailure()) {
                return Result.failure(tokenValidation.getError().orElse(
                    ErrorDetail.validationError("authToken", query.authToken(), "Invalid authentication token")
                ));
            }
            
            Result<BankId> bankIdResult = jwtTokenService.extractBankId(query.authToken());
            if (bankIdResult.isFailure()) {
                return Result.failure(bankIdResult.getError().orElse(
                    ErrorDetail.validationError("authToken", query.authToken(), "Cannot extract bank ID from token")
                ));
            }
            
            BankId requestingBankId = bankIdResult.getValue().orElseThrow();
            
            // 2. Find batch by ID
            IngestionBatch batch = ingestionBatchRepository.findByBatchId(query.batchId())
                .orElse(null);
            
            if (batch == null) {
                return Result.failure(ErrorDetail.businessRuleViolation("BATCH_NOT_FOUND", 
                    "Batch not found: " + query.batchId().value()));
            }
            
            // 3. Verify bank authorization (batch belongs to requesting bank)
            if (!batch.getBankId().equals(requestingBankId)) {
                return Result.failure(ErrorDetail.businessRuleViolation("UNAUTHORIZED_ACCESS", 
                    "Access denied: batch does not belong to requesting bank"));
            }
            
            // 4. Calculate progress and estimated completion time
            ProgressInfo progressInfo = calculateProgress(batch);
            
            // 5. Build performance metrics
            Map<String, Object> performanceMetrics = buildPerformanceMetrics(batch);
            
            // 6. Build download links (if applicable)
            Map<String, String> downloadLinks = buildDownloadLinks(batch);
            
            // 7. Build and return status DTO
            BatchStatusDto statusDto = BatchStatusDto.builder()
                .batchId(batch.getBatchId().value())
                .bankId(batch.getBankId().value())
                .status(batch.getStatus())
                .processingStage(getProcessingStage(batch.getStatus()))
                .progressPercentage(progressInfo.progressPercentage())
                .uploadedAt(batch.getUploadedAt())
                .completedAt(batch.getCompletedAt())
                .processingDurationMs(calculateProcessingDuration(batch))
                .estimatedCompletionTimeMs(progressInfo.estimatedCompletionTime())
                .fileName(batch.getFileMetadata().fileName())
                .contentType(batch.getFileMetadata().contentType())
                .fileSizeBytes(batch.getFileMetadata().fileSizeBytes())
                .totalExposures(batch.getTotalExposures())
                .s3Uri(batch.getS3Reference() != null ? batch.getS3Reference().uri() : null)
                .errorMessage(batch.getErrorMessage())
                .performanceMetrics(performanceMetrics)
                .downloadLinks(downloadLinks)
                .build();
            
            return Result.success(statusDto);
            
        } catch (Exception e) {
            return Result.failure(ErrorDetail.infrastructureError("STATUS_QUERY_HANDLER", 
                "Unexpected error retrieving batch status: " + e.getMessage()));
        }
    }
    
    private ProgressInfo calculateProgress(IngestionBatch batch) {
        BatchStatus status = batch.getStatus();
        Instant now = Instant.now();
        
        int progressPercentage = switch (status) {
            case UPLOADED -> 10;
            case PARSING -> 30;
            case VALIDATED -> 60;
            case STORING -> 80;
            case COMPLETED -> 100;
            case FAILED -> 0; // Failed batches show 0% progress
        };
        
        Long estimatedCompletionTime = null;
        
        // Calculate estimated completion time for in-progress batches
        if (status != BatchStatus.COMPLETED && status != BatchStatus.FAILED) {
            long elapsedMs = Duration.between(batch.getUploadedAt(), now).toMillis();
            long remainingMs = 0;
            
            // Add remaining time for current and future stages
            for (BatchStatus futureStatus : BatchStatus.values()) {
                if (futureStatus.ordinal() > status.ordinal()) {
                    remainingMs += ESTIMATED_STAGE_DURATIONS.getOrDefault(futureStatus, 0L);
                }
            }
            
            // Add partial time for current stage based on file size
            if (ESTIMATED_STAGE_DURATIONS.containsKey(status)) {
                long stageEstimate = ESTIMATED_STAGE_DURATIONS.get(status);
                // Adjust based on file size (larger files take longer)
                long fileSizeBytes = batch.getFileMetadata().fileSizeBytes();
                double sizeMultiplier = Math.max(1.0, fileSizeBytes / (10.0 * 1024 * 1024)); // Base: 10MB
                stageEstimate = (long) (stageEstimate * Math.min(sizeMultiplier, 5.0)); // Cap at 5x
                
                // Assume we're halfway through current stage
                remainingMs += stageEstimate / 2;
            }
            
            estimatedCompletionTime = now.toEpochMilli() + remainingMs;
        }
        
        return new ProgressInfo(progressPercentage, estimatedCompletionTime);
    }
    
    private String getProcessingStage(BatchStatus status) {
        return switch (status) {
            case UPLOADED -> "Queued";
            case PARSING -> "Parsing";
            case VALIDATED -> "Enriching";
            case STORING -> "Storing";
            case COMPLETED -> "Completed";
            case FAILED -> "Failed";
        };
    }
    
    private Long calculateProcessingDuration(IngestionBatch batch) {
        if (batch.getCompletedAt() != null) {
            return Duration.between(batch.getUploadedAt(), batch.getCompletedAt()).toMillis();
        } else if (batch.getStatus() != BatchStatus.UPLOADED) {
            return Duration.between(batch.getUploadedAt(), Instant.now()).toMillis();
        }
        return null;
    }
    
    private Map<String, Object> buildPerformanceMetrics(IngestionBatch batch) {
        Map<String, Object> metrics = new HashMap<>();
        
        // File size metrics
        long fileSizeBytes = batch.getFileMetadata().fileSizeBytes();
        metrics.put("fileSizeBytes", fileSizeBytes);
        metrics.put("fileSizeMB", fileSizeBytes / (1024.0 * 1024.0));
        
        // Processing metrics
        if (batch.getTotalExposures() != null) {
            metrics.put("totalExposures", batch.getTotalExposures());
            
            Long processingDuration = calculateProcessingDuration(batch);
            if (processingDuration != null && processingDuration > 0) {
                double exposuresPerSecond = batch.getTotalExposures() / (processingDuration / 1000.0);
                metrics.put("exposuresPerSecond", Math.round(exposuresPerSecond * 100.0) / 100.0);
                
                double mbPerSecond = (fileSizeBytes / (1024.0 * 1024.0)) / (processingDuration / 1000.0);
                metrics.put("mbPerSecond", Math.round(mbPerSecond * 100.0) / 100.0);
            }
        }
        
        // Status metrics
        metrics.put("currentStatus", batch.getStatus().name());
        metrics.put("uploadedAt", batch.getUploadedAt().toString());
        
        if (batch.getCompletedAt() != null) {
            metrics.put("completedAt", batch.getCompletedAt().toString());
        }
        
        return metrics;
    }
    
    private Map<String, String> buildDownloadLinks(IngestionBatch batch) {
        Map<String, String> links = new HashMap<>();
        
        if (batch.getStatus() == BatchStatus.COMPLETED && batch.getS3Reference() != null) {
            // In a real implementation, these would be pre-signed URLs or API endpoints
            links.put("originalFile", "/api/v1/ingestion/batch/" + batch.getBatchId().value() + "/download");
            links.put("processingReport", "/api/v1/ingestion/batch/" + batch.getBatchId().value() + "/report");
            links.put("validationSummary", "/api/v1/ingestion/batch/" + batch.getBatchId().value() + "/validation");
        }
        
        return links;
    }
    
    private record ProgressInfo(int progressPercentage, Long estimatedCompletionTime) {}
}