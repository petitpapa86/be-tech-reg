package com.bcbs239.regtech.ingestion.application.batch.upload;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommand;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Runs batch processing asynchronously.
 * <p>
 * Kept in a separate Spring bean so that @Async proxying applies.
 */
@Component
public class AsyncBatchProcessor {

    private final ProcessBatchCommandHandler processBatchCommandHandler;

    public AsyncBatchProcessor(ProcessBatchCommandHandler processBatchCommandHandler) {
        this.processBatchCommandHandler = processBatchCommandHandler;
    }

    @Async
    public CompletableFuture<Result<Void>> processBatchWithTempFile(BatchId batchId, String tempFileKey, String fileName) {
        ProcessBatchCommand processCommand = new ProcessBatchCommand(batchId, tempFileKey, fileName);
        Result<Void> processResult = processBatchCommandHandler.handle(processCommand);
        return CompletableFuture.completedFuture(processResult);
    }
}
