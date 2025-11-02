package com.bcbs239.regtech.modules.ingestion.presentation.batch.status;

import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.FieldError;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.application.batch.queries.BatchStatusDto;
import com.bcbs239.regtech.modules.ingestion.application.batch.queries.BatchStatusQuery;
import com.bcbs239.regtech.modules.ingestion.application.batch.queries.BatchStatusQueryHandler;
import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.modules.ingestion.infrastructure.security.IngestionSecurityService;
import com.bcbs239.regtech.modules.ingestion.presentation.common.IEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Functional endpoint for batch status queries.
 * Provides real-time status information for uploaded batches.
 */
@Component
public class BatchStatusController extends BaseController implements IEndpoint {
    
    private final BatchStatusQueryHandler batchStatusQueryHandler;
    private final IngestionSecurityService securityService;
    
    public BatchStatusController(BatchStatusQueryHandler batchStatusQueryHandler,
                               IngestionSecurityService securityService) {
        this.batchStatusQueryHandler = batchStatusQueryHandler;
        this.securityService = securityService;
    }
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return route(GET("/api/v1/ingestion/batch/{batchId}/status"), this::handle)
            .withAttribute("tags", new String[]{"Status Queries", "Ingestion"})
            .withAttribute("permissions", new String[]{"ingestion:status:view"});
    }
    
    private ServerResponse handle(ServerRequest request) {
        // Extract batch ID from path variable
        String batchIdStr = request.pathVariable("batchId");
        
        // Extract auth token from header
        String authToken = request.headers().firstHeader("Authorization");
        
        // Validate JWT token and extract bank ID using existing security infrastructure
        Result<BankId> bankIdResult = securityService.validateTokenAndExtractBankId(authToken);
        if (bankIdResult.isFailure()) {
            ErrorDetail error = bankIdResult.getError().orElseThrow();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
        
        BankId bankId = bankIdResult.getValue().orElseThrow();
        
        // Verify ingestion status permissions using existing security infrastructure
        Result<Void> permissionResult = securityService.verifyIngestionPermissions("status");
        if (permissionResult.isFailure()) {
            ErrorDetail error = permissionResult.getError().orElseThrow();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
        
        // Validate request parameters
        Result<Void> validationResult = validateStatusRequest(batchIdStr, authToken);
        if (validationResult.isFailure()) {
            ErrorDetail error = validationResult.getError().orElseThrow();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
        
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

        return Result.success();
    }
}