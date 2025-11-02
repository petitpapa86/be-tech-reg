package com.bcbs239.regtech.ingestion.web.controller;

import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.FieldError;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import com.bcbs239.regtech.ingestion.application.command.processbatch.ProcessBatchCommand;
import com.bcbs239.regtech.ingestion.application.command.processbatch.ProcessBatchCommandHandler;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.web.endpoint.IEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Functional endpoint for batch processing operations.
 * Handles asynchronous processing of uploaded batches.
 */
@Component
public class ProcessBatchController extends BaseController implements IEndpoint {
    
    private final ProcessBatchCommandHandler processBatchCommandHandler;
    
    public ProcessBatchController(ProcessBatchCommandHandler processBatchCommandHandler) {
        this.processBatchCommandHandler = processBatchCommandHandler;
    }
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return route(POST("/api/v1/ingestion/batch/{batchId}/process"), this::handle)
            .withAttribute("tags", new String[]{"Batch Processing", "Ingestion"})
            .withAttribute("permissions", new String[]{"ingestion:process"});
    }
    
    private ServerResponse handle(ServerRequest request) {
        // Extract batch ID from path variable
        String batchIdStr = request.pathVariable("batchId");
        
        // Extract multipart file
        var multipartData = request.multipartData();
        var filePart = multipartData.get("file");
        
        // Validate request parameters
        Result<Void> validationResult = validateProcessRequest(batchIdStr, filePart);
        if (validationResult.isFailure()) {
            ErrorDetail error = validationResult.getError().orElseThrow();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
        
        var file = filePart.get(0);
        
        // Create command - IOException and IllegalArgumentException will be caught by IngestionExceptionHandler
        ProcessBatchCommand command = new ProcessBatchCommand(
            BatchId.of(batchIdStr),
            file.getInputStream()
        );
        
        // Execute command
        Result<Void> result = processBatchCommandHandler.handle(command);
        
        if (result.isSuccess()) {
            ProcessBatchResponse response = ProcessBatchResponse.from(batchIdStr);
            return ServerResponse.ok()
                .body(ResponseUtils.success(response, "Batch processing completed successfully"));
        } else {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(result, 
                "Batch processing completed", "ingestion.process.success");
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
    }

    /**
     * Validates process request parameters.
     */
    private Result<Void> validateProcessRequest(String batchId, List<org.springframework.web.multipart.MultipartFile> filePart) {
        List<FieldError> fieldErrors = new java.util.ArrayList<>();

        // Validate batch ID
        if (batchId == null || batchId.trim().isEmpty()) {
            fieldErrors.add(new FieldError("batchId", "REQUIRED", 
                "Batch ID cannot be empty"));
        }

        // Validate file parameter
        if (filePart == null || filePart.isEmpty()) {
            fieldErrors.add(new FieldError("file", "REQUIRED", 
                "File parameter is required"));
        }

        if (!fieldErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }

        return Result.success();
    }
    
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
}