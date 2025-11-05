package com.bcbs239.regtech.ingestion.presentation.batch.process;

/**
 * Response DTO for successful batch processing.
 */
public record ProcessBatchResponse(
    String batchId,
    String message,
    String status
) {
    public static ProcessBatchResponse from(String batchId) {
        return new ProcessBatchResponse(
            batchId,
            "Batch processing completed successfully",
            "COMPLETED"
        );
    }
}

