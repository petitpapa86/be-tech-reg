package com.bcbs239.regtech.ingestion.presentation.batch.upload;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.controllers.BaseController;
import com.bcbs239.regtech.ingestion.application.batch.upload.UploadAndProcessFileCommand;
import com.bcbs239.regtech.ingestion.application.batch.upload.UploadAndProcessFileCommandHandler;
import com.bcbs239.regtech.ingestion.presentation.common.MultipartFileUtils;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import com.bcbs239.regtech.ingestion.domain.file.FileName;



@Component
@Slf4j
public class UploadAndProcessFileController extends BaseController {

    private final UploadAndProcessFileCommandHandler uploadAndProcessFileCommandHandler;
    private final MultipartFileUtils multipartFileUtils;
    private final long maxFileSizeBytes;

    public UploadAndProcessFileController(
            UploadAndProcessFileCommandHandler uploadAndProcessFileCommandHandler,
            MultipartFileUtils multipartFileUtils,
            @Value("${ingestion.file.max-size}") long maxFileSizeBytes) {
        this.uploadAndProcessFileCommandHandler = uploadAndProcessFileCommandHandler;
        this.multipartFileUtils = multipartFileUtils;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Observed(name = "ingestion.api.upload.process", contextualName = "upload-and-process-file")
    public ServerResponse handle(ServerRequest request) {
        String bankIdValue = request.headers().firstHeader("X-Bank-Id");

        Result<org.springframework.util.MultiValueMap<String, jakarta.servlet.http.Part>> multipartResult =
                multipartFileUtils.extractMultipartData(request);
        if (multipartResult.isFailure()) {
            ResponseEntity<? extends ApiResponse<?>> res = handleResult(multipartResult, "file.upload.process");
            return ServerResponse.status(res.getStatusCode()).body(res);
        }

        Result<jakarta.servlet.http.Part> fileResult = multipartFileUtils.extractAndValidateFile(
                multipartResult.getValue().orElseThrow(),
                this.maxFileSizeBytes,
                java.util.List.of("application/json", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        );

        if (fileResult.isFailure()) {
            ResponseEntity<? extends ApiResponse<?>> res = handleResult(fileResult, "file.upload.process");
            return ServerResponse.status(res.getStatusCode()).body(res);
        }

        var file = fileResult.getValue().orElseThrow();

        String batchIdValue = request.param("batchId").orElse(null);

        UploadAndProcessFileCommand command;
        try {
            // Validate and build FileName value object at the boundary
            Result<FileName> fileNameResult = FileName.create(file.getSubmittedFileName());
            if (fileNameResult.isFailure()) {
                ResponseEntity<? extends ApiResponse<?>> res = handleResult(fileNameResult, "file.upload.process");
                return ServerResponse.status(res.getStatusCode()).body(res);
            }
            FileName fileNameVo = fileNameResult.getValue().orElseThrow();

            command = new UploadAndProcessFileCommand(
                    file.getInputStream(),
                    fileNameVo,
                    file.getContentType(),
                    file.getSize(), bankIdValue,
                    batchIdValue
            );
        } catch (IOException e) {
            return handleSystemErrorResponse(e);
        }

        Result<BatchId> result = uploadAndProcessFileCommandHandler.handle(command);

        ResponseEntity<? extends ApiResponse<?>> res = handleResult(result, "file.upload.process");
        return ServerResponse.status(res.getStatusCode()).body(res);
    }
}