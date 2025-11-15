package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.ingestion.domain.batch.BatchId;

/**
 * Command for processing a batch asynchronously.
 * Contains batch ID and temporary file reference key for processing.
 */
public record ProcessBatchCommand(
    BatchId batchId,
    String tempFileKey
) {
    
    public ProcessBatchCommand {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (tempFileKey == null || tempFileKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Temporary file key cannot be null or empty");
        }
    }
}


