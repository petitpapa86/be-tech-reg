package com.bcbs239.regtech.ingestion.application.batch.upload;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommand;
import com.bcbs239.regtech.ingestion.application.batch.process.ProcessBatchCommandHandler;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Runs batch processing asynchronously.
 *
 * Kept in a separate Spring bean so that @Async proxying applies.
 */
@Component
public class AsyncBatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncBatchProcessor.class);

    private final ProcessBatchCommandHandler processBatchCommandHandler;

    public AsyncBatchProcessor(ProcessBatchCommandHandler processBatchCommandHandler) {
        this.processBatchCommandHandler = processBatchCommandHandler;
    }

    @Async
    public CompletableFuture<Result<Void>> processBatchWithTempFile(BatchId batchId, String tempFileKey) {
        try {
            ProcessBatchCommand processCommand = new ProcessBatchCommand(batchId, tempFileKey);
            Result<Void> processResult = processBatchCommandHandler.handle(processCommand);
            return CompletableFuture.completedFuture(processResult);
        } catch (Exception e) {
            log.error("Failed to process batch with temporary file; details={}",
                Map.of("batchId", batchId.value(), "errorMessage", String.valueOf(e.getMessage())), e);
            return CompletableFuture.completedFuture(Result.failure(ErrorDetail.of(
                "PROCESS_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to process batch: " + e.getMessage(),
                "batch.process.error"
            )));
        }
    }
}
