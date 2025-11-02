package com.bcbs239.regtech.modules.ingestion.presentation.batch.upload;

import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId;

/**
 * Response DTO for successful file upload.
 */
public record UploadFileResponse(
    String batchId,
    String message,
    String status,
    String statusUrl
) {
    public static UploadFileResponse from(BatchId batchId) {
        return new UploadFileResponse(
            batchId.value(),
            "File uploaded successfully and queued for processing",
            "UPLOADED",
            "/api/v1/ingestion/batch/" + batchId.value() + "/status"
        );
    }
}