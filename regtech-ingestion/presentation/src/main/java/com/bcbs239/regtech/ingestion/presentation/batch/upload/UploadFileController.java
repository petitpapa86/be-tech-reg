package com.bcbs239.regtech.ingestion.presentation.batch.upload;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.presentation.common.IEndpoint;
import com.bcbs239.regtech.ingestion.presentation.constants.Permissions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.List;

import static com.bcbs239.regtech.core.presentation.routing.RouterAttributes.withAttributes;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Functional endpoint for file upload operations.
 * Handles file uploads for ingestion processing with proper validation and rate limiting.
 */
@Component
@Slf4j
public class UploadFileController extends BaseController implements IEndpoint {
    
    private final UploadFileCommandHandler uploadFileCommandHandler;
    
    public UploadFileController(UploadFileCommandHandler uploadFileCommandHandler) {
        this.uploadFileCommandHandler = uploadFileCommandHandler;
    }
    
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        return withAttributes(
            route(POST("/api/v1/ingestion/upload"), this::handle),
            new String[]{Permissions.UPLOAD_FILE},
            new String[]{"File Upload", "Ingestion"},
            "Upload a file for ingestion processing with validation and rate limiting"
        );
    }
    
    private ServerResponse handle(ServerRequest request) {
        // Extract bank ID from header (assuming it's passed directly now that JWT validation is removed)
        String bankIdValue = request.headers().firstHeader("X-Bank-Id");
        if (bankIdValue == null || bankIdValue.trim().isEmpty()) {
            return ServerResponse.badRequest()
                .body(ResponseUtils.error("Bank ID is required", "BANK_ID_MISSING"));
        }
        
        BankId bankId;
        try {
            bankId = BankId.of(bankIdValue.trim());
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .body(ResponseUtils.error("Invalid bank ID format", "INVALID_BANK_ID"));
        }
        
        // Extract multipart file
        org.springframework.util.MultiValueMap<String, jakarta.servlet.http.Part> multipartData;
        java.util.List<jakarta.servlet.http.Part> filePart;
        try {
            multipartData = request.multipartData();
            filePart = multipartData.get("file");
        } catch (IOException | jakarta.servlet.ServletException e) {
            throw new RuntimeException("Failed to process multipart request", e);
        }
        
        // Validate request parameters
        Result<Void> validationResult = validateUploadRequest(filePart);
        if (validationResult.isFailure()) {
            ErrorDetail error = validationResult.getError().orElseThrow();
            
            // Handle file too large case with HTTP 413
            if (error.hasFieldErrors() && error.getFieldErrors().stream()
                .anyMatch(fe -> "FILE_TOO_LARGE".equals(fe.field()))) {
                return ServerResponse.status(413)
                    .body(ResponseUtils.validationError(error.getFieldErrors(), error.getMessage()));
            }
            
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleError(error);
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
        
        var file = filePart.get(0);
        
        // Create command - IOException will be caught by IngestionExceptionHandler
        UploadFileCommand command;
        try {
            command = new UploadFileCommand(
                file.getInputStream(),
                file.getSubmittedFileName(),
                file.getContentType(),
                file.getSize(),
                bankId
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
        
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
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
    }
    
    /**
     * Validates upload request parameters.
     */
    private Result<Void> validateUploadRequest(List<jakarta.servlet.http.Part> filePart) {
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
                return Result.failure(ErrorDetail.of("FILE_TOO_LARGE", ErrorType.VALIDATION_ERROR,
                    "File size exceeds maximum allowed limit", "file.upload.size.exceeded"));
            }

            // Validate content type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/json") && 
                !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))) {
                fieldErrors.add(new FieldError("file", "INVALID_CONTENT_TYPE", 
                    "Only JSON and Excel files are supported"));
            }

            // Validate file name
            String fileName = file.getSubmittedFileName();
            if (fileName == null || fileName.trim().isEmpty()) {
                fieldErrors.add(new FieldError("file", "INVALID_FILENAME", 
                    "File must have a valid name"));
            }
        }

        if (!fieldErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }

        return Result.success(null);
    }
}

