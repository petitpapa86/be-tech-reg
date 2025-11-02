package com.bcbs239.regtech.modules.ingestion.presentation.batch.upload;

import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.FieldError;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import com.bcbs239.regtech.modules.ingestion.application.batch.upload.UploadFileCommand;
import com.bcbs239.regtech.modules.ingestion.application.batch.upload.UploadFileCommandHandler;
import com.bcbs239.regtech.modules.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.modules.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.modules.ingestion.infrastructure.security.IngestionSecurityService;
import com.bcbs239.regtech.modules.ingestion.presentation.common.IEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.List;

import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static com.bcbs239.regtech.core.web.RouterAttributes.*;

/**
 * Functional endpoint for file upload operations.
 * Handles file uploads for ingestion processing with proper validation and rate limiting.
 */
@Component
public class UploadFileController extends BaseController implements IEndpoint {
    
    private final UploadFileCommandHandler uploadFileCommandHandler;
    private final IngestionSecurityService securityService;
    
    public UploadFileController(UploadFileCommandHandler uploadFileCommandHandler,
                              IngestionSecurityService securityService) {
        this.uploadFileCommandHandler = uploadFileCommandHandler;
        this.securityService = securityService;
    }
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return withAttributes(
            route(POST("/api/v1/ingestion/upload"), this::handle),
            new String[]{"ingestion:upload"},
            new String[]{"File Upload", "Ingestion"},
            "Upload a file for ingestion processing with validation and rate limiting"
        );
    }
    
    private ServerResponse handle(ServerRequest request) {
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
        
        // Verify ingestion permissions using existing security infrastructure
        Result<Void> permissionResult = securityService.verifyIngestionPermissions("upload");
        if (permissionResult.isFailure()) {
            ErrorDetail error = permissionResult.getError().orElseThrow();
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
        
        // Extract multipart file
        var multipartData = request.multipartData();
        var filePart = multipartData.get("file");
        
        // Validate request parameters
        Result<Void> validationResult = validateUploadRequest(filePart, authToken);
        if (validationResult.isFailure()) {
            ErrorDetail error = validationResult.getError().orElseThrow();
            
            // Handle file too large case with HTTP 413
            if (error.hasFieldErrors() && error.getFieldErrors().stream()
                .anyMatch(fe -> "FILE_TOO_LARGE".equals(fe.getCode()))) {
                return ServerResponse.status(413)
                    .body(ResponseUtils.validationError(error.getFieldErrors(), error.getMessage()));
            }
            
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
        
        var file = filePart.get(0);
        
        // Create command - IOException will be caught by IngestionExceptionHandler
        UploadFileCommand command = new UploadFileCommand(
            file.getInputStream(),
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            authToken
        );
        
        // Execute command
        Result<BatchId> result = uploadFileCommandHandler.handle(command);
        
        if (result.isSuccess()) {
            BatchId batchId = result.getValue().orElseThrow();
            UploadFileResponse response = UploadFileResponse.from(batchId);
            
            // Return HTTP 202 Accepted for asynchronous processing
            return ServerResponse.accepted()
                .body(ResponseUtils.success(response, "File uploaded successfully and queued for processing"));
        } else {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(result, 
                "File uploaded successfully", "ingestion.upload.success");
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
    }
    
    /**
     * Validates upload request parameters.
     */
    private Result<Void> validateUploadRequest(List<org.springframework.web.multipart.MultipartFile> filePart, String authToken) {
        List<FieldError> fieldErrors = new java.util.ArrayList<>();

        // Validate file parameter
        if (filePart == null || filePart.isEmpty()) {
            fieldErrors.add(new FieldError("file", "REQUIRED", "File parameter is required"));
        } else {
            var file = filePart.get(0);
            
            // Validate file size (500MB limit) - return special error for HTTP 413
            long maxFileSize = 500L * 1024 * 1024; // 500MB in bytes
            if (file.getSize() > maxFileSize) {
                List<FieldError> sizeErrors = List.of(new FieldError("file", "FILE_TOO_LARGE", 
                    "File size exceeds maximum limit of 500MB. Consider splitting the file into smaller chunks."));
                return Result.failure(ErrorDetail.validationError(sizeErrors, 
                    "File size exceeds maximum allowed limit"));
            }

            // Validate content type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/json") && 
                !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))) {
                fieldErrors.add(new FieldError("file", "INVALID_CONTENT_TYPE", 
                    "Only JSON and Excel files are supported"));
            }

            // Validate file name
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.trim().isEmpty()) {
                fieldErrors.add(new FieldError("file", "INVALID_FILENAME", 
                    "File must have a valid name"));
            }
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