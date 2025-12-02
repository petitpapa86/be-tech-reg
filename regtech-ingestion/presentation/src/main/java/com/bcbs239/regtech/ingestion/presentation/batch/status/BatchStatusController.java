package com.bcbs239.regtech.ingestion.presentation.batch.status;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.ingestion.application.batch.queries.BatchStatusDto;
import com.bcbs239.regtech.ingestion.application.batch.queries.BatchStatusQuery;
import com.bcbs239.regtech.ingestion.application.batch.queries.BatchStatusQueryHandler;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.presentation.common.IEndpoint;
import com.bcbs239.regtech.ingestion.presentation.constants.Tags;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Functional endpoint for batch status queries.
 * Provides real-time status information for uploaded batches.
 */
@Component("ingestionBatchStatusController")
public class BatchStatusController extends BaseController implements IEndpoint {
    
    private final BatchStatusQueryHandler batchStatusQueryHandler;

    public BatchStatusController(BatchStatusQueryHandler batchStatusQueryHandler) {
        this.batchStatusQueryHandler = batchStatusQueryHandler;
    }
    
    /**
     * Maps the batch status query endpoints.
     * Requires authentication and specific permission to view batch status.
     * 
     * Requirements: 12.2, 12.3, 12.4
     */
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/ingestion/batch/{batchId}/status"), this::handle),
            new String[]{"ingestion:status:view"},
            new String[]{Tags.INGESTION, Tags.STATUS},
            "Get batch processing status by batch ID"
        ).and(RouterAttributes.withAttributes(
            route(POST("/api/v1/ingestion/batch/{batchId}/status"), this::handle),
            new String[]{"ingestion:status:view"},
            new String[]{Tags.INGESTION, Tags.STATUS},
            "Get batch processing status by batch ID (POST method for compatibility)"
        ));
    }
    
    private ServerResponse handle(ServerRequest request) {
        // Extract batch ID from path variable
        String batchIdStr = request.pathVariable("batchId");
        
        // Extract auth token from header
        String authToken = request.headers().firstHeader("Authorization");

        // Validate request parameters
//        Result<Void> validationResult = validateStatusRequest(batchIdStr, authToken);
//        if (validationResult.isFailure()) {
//            ErrorDetail error = validationResult.getError().orElseThrow();
//            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
//            assert responseEntity.getBody() != null;
//            return ServerResponse.status(responseEntity.getStatusCode())
//                .body(responseEntity.getBody());
//        }
        
        // Create query - IllegalArgumentException will be caught by IngestionExceptionHandler
        BatchStatusQuery query = new BatchStatusQuery(
            BatchId.of(batchIdStr),
            authToken
        );
        
        // Execute query
        Result<BatchStatusDto> result = batchStatusQueryHandler.handle(query);
        
        // Handle result using BaseController infrastructure
        ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(result, 
            "Batch status retrieved successfully", "ingestion.status.success");

        assert responseEntity.getBody() != null;
        return ServerResponse.status(responseEntity.getStatusCode())
            .body(responseEntity.getBody());
    }

    /**
     * Validates status request parameters.
     */
    private Result<Void> validateStatusRequest(String batchId, String authToken) {
        List<FieldError> fieldErrors = new java.util.ArrayList<>();

        // Validate batch ID
        if (batchId == null || batchId.trim().isEmpty()) {
            fieldErrors.add(new FieldError("batchId", "REQUIRED", 
                "Batch ID cannot be empty"));
        }

        // Validate authorization header
        if (authToken == null || authToken.trim().isEmpty()) {
            fieldErrors.add(new FieldError("Authorization", "REQUIRED", 
                "Authorization header is required"));
        }

        if (!fieldErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }

        return Result.success(null);
    }
}

