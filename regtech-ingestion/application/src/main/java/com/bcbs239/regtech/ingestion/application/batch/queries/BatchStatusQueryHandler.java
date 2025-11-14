package com.bcbs239.regtech.ingestion.application.batch.queries;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Query handler for retrieving batch status and processing information.
 * Handles authentication, authorization, and status calculation.
 */
@Component
public class BatchStatusQueryHandler {
    
    private final IIngestionBatchRepository ingestionBatchRepository;
    private final IngestionSecurityService securityService;
    private final IngestionLoggingService loggingService;
    
    // Estimated processing times per stage
    private static final Map<BatchStatus, ProcessingDuration> ESTIMATED_STAGE_DURATIONS = Map.of(
        BatchStatus.PARSING, new ProcessingDuration(30_000L),      // 30 seconds
        BatchStatus.VALIDATED, new ProcessingDuration(15_000L),    // 15 seconds  
        BatchStatus.STORING, new ProcessingDuration(60_000L)       // 60 seconds
    );
    
    public BatchStatusQueryHandler(
            IIngestionBatchRepository ingestionBatchRepository,
            IngestionSecurityService securityService,
            IngestionLoggingService loggingService) {
        this.ingestionBatchRepository = ingestionBatchRepository;
        this.securityService = securityService;
        this.loggingService = loggingService;
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
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Validate JWT token and extract bank ID using existing security infrastructure
            Result<BankId> bankIdResult = securityService.validateTokenAndExtractBankId(query.authToken());
            if (bankIdResult.isFailure()) {
                return Result.failure(bankIdResult.getError().orElseThrow());
            }
            
            BankId requestingBankId = bankIdResult.getValue().orElseThrow();
            
            // 2. Find batch by ID
            IngestionBatch batch = ingestionBatchRepository.findByBatchId(query.batchId())
                .orElse(null);
            
            if (batch == null) {
                // Log access attempt for audit trail
                Map<String, Object> context = Map.of(
                    "batchId", query.batchId().value(),
                    "requestingBankId", requestingBankId.value(),
                    "result", "batch_not_found"
                );
                loggingService.logRequestFlowStep("BATCH_STATUS_QUERY", "BATCH_NOT_FOUND", context);
                
                return Result.failure(ErrorDetail.of("BATCH_NOT_FOUND", ErrorType.NOT_FOUND_ERROR,
                    "Batch not found: " + query.batchId().value(), "batch.status.not.found"));
            }
            
            // 3. Verify batch access permissions using existing security infrastructure
            Result<Void> accessResult = securityService.verifyBatchAccess(query.batchId(), batch.getBankId());
            if (accessResult.isFailure()) {
                return Result.failure(accessResult.getError().orElseThrow());
            }
            
            // 4. Calculate progress and estimated completion time
            ProgressInfo progressInfo = calculateProgress(batch);
            
            // 5. Build performance metrics
            Map<String, Object> performanceMetrics = buildPerformanceMetrics(batch);
            
            // 6. Build download links (if applicable)
            Map<String, String> downloadLinks = buildDownloadLinks(batch);
            
            // 7. Log successful status query for audit trail
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> context = Map.of(
                "batchId", batch.getBatchId().value(),
                "requestingBankId", requestingBankId.value(),
                "batchStatus", batch.getStatus().name(),
                "durationMs", duration,
                "result", "success"
            );
            loggingService.logRequestFlowStep("BATCH_STATUS_QUERY", "STATUS_RETRIEVED", context);
            
            // 8. Build and return status DTO
            ProcessingStage processingStage = ProcessingStage.fromBatchStatus(batch.getStatus());
            ProcessingDuration processingDuration = calculateProcessingDuration(batch);
            
            BatchStatusDto statusDto = BatchStatusDto.builder()
                .batchId(batch.getBatchId().value())
                .bankId(batch.getBankId().value())
                .status(batch.getStatus())
                .processingStage(processingStage.value())
                .progressPercentage(progressInfo.progressPercentage().value())
                .uploadedAt(batch.getUploadedAt())
                .completedAt(batch.getCompletedAt())
                .processingDurationMs(processingDuration != null ? processingDuration.milliseconds() : null)
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
            return Result.failure(ErrorDetail.of("STATUS_QUERY_HANDLER_ERROR", ErrorType.SYSTEM_ERROR,
                "Unexpected error retrieving batch status: " + e.getMessage(), "batch.status.query.error"));
        }
    }
    
    private ProgressInfo calculateProgress(IngestionBatch batch) {
        BatchStatus status = batch.getStatus();
        
        // Use domain value object to calculate progress from status
        ProgressPercentage progressPercentage = ProgressPercentage.fromBatchStatus(status);
        
        // Use domain value object to calculate estimated completion time
        FileSizeBytes fileSize = FileSizeBytes.create(batch.getFileMetadata().fileSizeBytes())
            .getValue().orElseThrow();
        FileSizeBytes baseSize = FileSizeBytes.fromMB(10); // Base: 10MB for estimation
        
        Maybe<EstimatedCompletionTime> estimatedCompletion = EstimatedCompletionTime.calculate(
            status,
            ESTIMATED_STAGE_DURATIONS,
            fileSize,
            baseSize
        );
        
        Long estimatedCompletionTime = estimatedCompletion.isPresent() 
            ? estimatedCompletion.getValue().epochMillis() 
            : null;
        
        return new ProgressInfo(progressPercentage, estimatedCompletionTime);
    }
    
    private ProcessingDuration calculateProcessingDuration(IngestionBatch batch) {
        if (batch.getCompletedAt() != null) {
            return ProcessingDuration.between(batch.getUploadedAt(), batch.getCompletedAt())
                .getValue().orElse(ProcessingDuration.zero());
        } else if (batch.getStatus() != BatchStatus.UPLOADED) {
            return ProcessingDuration.fromStart(batch.getUploadedAt())
                .getValue().orElse(ProcessingDuration.zero());
        }
        return null;
    }
    
    private Map<String, Object> buildPerformanceMetrics(IngestionBatch batch) {
        Map<String, Object> metrics = new HashMap<>();
        
        // File size metrics using value object
        FileSizeBytes fileSize = FileSizeBytes.create(batch.getFileMetadata().fileSizeBytes())
            .getValue().orElseThrow();
        metrics.put("fileSizeBytes", fileSize.value());
        metrics.put("fileSizeMB", fileSize.toMB());
        
        // Processing metrics
        if (batch.getTotalExposures() != null) {
            metrics.put("totalExposures", batch.getTotalExposures());
            
            ProcessingDuration processingDuration = calculateProcessingDuration(batch);
            if (processingDuration != null && !processingDuration.isZero()) {
                // Calculate throughput metrics using value object
                Result<ThroughputMetrics> throughputResult = ThroughputMetrics.calculate(
                    batch.getTotalExposures(),
                    fileSize,
                    processingDuration
                );
                
                if (throughputResult.isSuccess()) {
                    ThroughputMetrics throughput = throughputResult.getValue().orElseThrow();
                    metrics.put("exposuresPerSecond", throughput.recordsPerSecond());
                    metrics.put("mbPerSecond", throughput.megabytesPerSecond());
                }
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
    
    private record ProgressInfo(ProgressPercentage progressPercentage, Long estimatedCompletionTime) {}
    
    // These services will be migrated to infrastructure layer
    // For now, creating placeholder interfaces
    public interface IngestionSecurityService {
        Result<BankId> validateTokenAndExtractBankId(String token);
        Result<Void> verifyBatchAccess(BatchId batchId, BankId bankId);
    }
    
    public interface IngestionLoggingService {
        void logRequestFlowStep(String operation, String step, Map<String, Object> context);
    }
}

