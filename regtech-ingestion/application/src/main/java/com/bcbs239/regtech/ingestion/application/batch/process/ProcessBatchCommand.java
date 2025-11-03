package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.ingestion.domain.batch.BatchId;

import java.io.InputStream;

/**
 * Command for processing a batch asynchronously.
 * Contains batch ID and file stream for processing.
 */
public record ProcessBatchCommand(
    BatchId batchId,
    InputStream fileStream
) {
    
    public ProcessBatchCommand {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (fileStream == null) {
            throw new IllegalArgumentException("File stream cannot be null");
        }
    }
}