package com.bcbs239.regtech.ingestion.presentation.batch.upload;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.ingestion.application.batch.upload.UploadAndProcessFileCommand;
import com.bcbs239.regtech.ingestion.application.batch.upload.UploadAndProcessFileCommandHandler;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.presentation.common.MultipartFileUtils;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;

import static com.bcbs239.regtech.core.presentation.routing.RouterAttributes.withAttributes;
import static org.springframework.web.servlet.function.RequestPredicates.POST;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Combined endpoint for file upload and immediate processing operations.
 * Handles file uploads with immediate asynchronous processing in a single request.
 */
@Component
@Slf4j
public class UploadAndProcessFileController extends BaseController {

    private final UploadAndProcessFileCommandHandler uploadAndProcessFileCommandHandler;
    private final MultipartFileUtils multipartFileUtils;

    public UploadAndProcessFileController(
            UploadAndProcessFileCommandHandler uploadAndProcessFileCommandHandler,
            MultipartFileUtils multipartFileUtils) {
        this.uploadAndProcessFileCommandHandler = uploadAndProcessFileCommandHandler;
        this.multipartFileUtils = multipartFileUtils;
    }

    @Observed(name = "ingestion.api.upload.process", contextualName = "upload-and-process-file")
    public ServerResponse handle(ServerRequest request) {
        String bankIdValue = request.headers().firstHeader("X-Bank-Id");
        if (bankIdValue == null || bankIdValue.trim().isEmpty()) {
            return ServerResponse.badRequest()
                .body(ApiResponse.businessRuleError("Bank ID is required"));
        }

        BankId bankId;
        try {
            bankId = BankId.of(bankIdValue.trim());
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .body(ApiResponse.businessRuleError("Invalid bank ID format"));
        }

        // Extract and validate multipart file
        Result<org.springframework.util.MultiValueMap<String, jakarta.servlet.http.Part>> multipartResult =
            multipartFileUtils.extractMultipartData(request);
        if (multipartResult.isFailure()) {
            throw new RuntimeException(multipartResult.getError().orElseThrow().getMessage());
        }

        Result<jakarta.servlet.http.Part> fileResult = multipartFileUtils.extractAndValidateFile(
            multipartResult.getValue().orElseThrow(),
            500L * 1024 * 1024, // 500MB
            java.util.List.of("application/json", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        );

        if (fileResult.isFailure()) {
            ErrorDetail error = fileResult.getError().orElseThrow();

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

        var file = fileResult.getValue().orElseThrow();

        // Read batchId provided by frontend (header X-Batch-Id)
        String batchIdValue = request.headers().firstHeader("X-Batch-Id");
        if (batchIdValue == null || batchIdValue.trim().isEmpty()) {
            return ServerResponse.badRequest()
                .body(ApiResponse.businessRuleError("Batch ID is required (header: X-Batch-Id)"));
        }

        BatchId batchId;
        try {
            batchId = BatchId.of(batchIdValue.trim());
        } catch (Exception e) {
            return ServerResponse.badRequest()
                .body(ApiResponse.businessRuleError("Invalid batch ID format"));
        }

        // Create command
        UploadAndProcessFileCommand command;
        try {
            command = new UploadAndProcessFileCommand(
                file.getInputStream(),
                file.getSubmittedFileName(),
                file.getContentType(),
                file.getSize(),
                bankId,
                batchId
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        // Execute command
        Result<com.bcbs239.regtech.ingestion.domain.batch.BatchId> result =
            uploadAndProcessFileCommandHandler.handle(command);

        if (result.isSuccess()) {
            UploadFileResponse response = UploadFileResponse.from(batchId);

            // Return HTTP 202 Accepted for asynchronous processing
            return ServerResponse.accepted()
                .body(ResponseUtils.success(response, "File uploaded and processing started successfully"));
        } else {
            ResponseEntity<? extends ApiResponse<?>> responseEntity = handleResult(result,
                "File uploaded and processing completed", "ingestion.upload.process.success");
            assert responseEntity.getBody() != null;
            return ServerResponse.status(responseEntity.getStatusCode())
                .body(responseEntity.getBody());
        }
    }
}