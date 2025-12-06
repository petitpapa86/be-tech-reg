package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.core.application.Command;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;

import java.util.Objects;

/**
 * Command for processing a batch asynchronously.
 * Contains batch ID and temporary file reference key for processing.
 */
public final class ProcessBatchCommand extends Command {
    private final BatchId batchId;
    private final String tempFileKey;


    public ProcessBatchCommand(BatchId batchId, String tempFileKey) {
        if (batchId == null) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
        if (tempFileKey == null || tempFileKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Temporary file key cannot be null or empty");
        }
        this.batchId = batchId;
        this.tempFileKey = tempFileKey;
    }

    public BatchId batchId() {
        return batchId;
    }

    public String tempFileKey() {
        return tempFileKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ProcessBatchCommand) obj;
        return Objects.equals(this.batchId, that.batchId) &&
                Objects.equals(this.tempFileKey, that.tempFileKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId, tempFileKey);
    }

    @Override
    public String toString() {
        return "ProcessBatchCommand[" +
                "batchId=" + batchId + ", " +
                "tempFileKey=" + tempFileKey + ']';
    }

}


