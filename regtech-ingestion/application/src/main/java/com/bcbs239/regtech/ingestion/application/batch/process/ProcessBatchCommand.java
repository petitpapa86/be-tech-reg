package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.core.application.Command;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class ProcessBatchCommand extends Command {
    private final BatchId batchId;
    private final String tempFileKey;
    private final String fileName;


    public ProcessBatchCommand(BatchId batchId, String tempFileKey, String fileName) {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (tempFileKey == null || tempFileKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Temporary file key cannot be null or empty");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        this.fileName = fileName;
        this.batchId = batchId;
        this.tempFileKey = tempFileKey;
    }


}
