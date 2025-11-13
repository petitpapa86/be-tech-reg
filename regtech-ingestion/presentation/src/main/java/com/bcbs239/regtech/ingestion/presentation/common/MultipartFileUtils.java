package com.bcbs239.regtech.ingestion.presentation.common;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Utility class for handling multipart file operations across controllers.
 * Provides common validation and extraction logic to reduce duplication.
 */
@Component
@Slf4j
public class MultipartFileUtils {

    /**
     * Extracts and validates a file from multipart data.
     * Performs basic validation (presence, size limits, content type).
     */
    public Result<jakarta.servlet.http.Part> extractAndValidateFile(
            org.springframework.util.MultiValueMap<String, jakarta.servlet.http.Part> multipartData,
            long maxFileSizeBytes,
            List<String> allowedContentTypes) {

        List<jakarta.servlet.http.Part> fileParts = multipartData.get("file");

        // Check if file is present
        if (fileParts == null || fileParts.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(
                List.of(new FieldError("file", "REQUIRED", "File parameter is required"))
            ));
        }

        jakarta.servlet.http.Part file = fileParts.get(0);

        // Validate file size
        if (file.getSize() > maxFileSizeBytes) {
            return Result.failure(ErrorDetail.of("FILE_TOO_LARGE", ErrorType.VALIDATION_ERROR,
                String.format("File size exceeds maximum limit of %d MB", maxFileSizeBytes / (1024 * 1024)),
                "file.upload.size.exceeded"));
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            return Result.failure(ErrorDetail.validationError(
                List.of(new FieldError("file", "INVALID_CONTENT_TYPE",
                    "Unsupported content type. Allowed: " + String.join(", ", allowedContentTypes)))
            ));
        }

        // Validate filename
        String fileName = file.getSubmittedFileName();
        if (fileName == null || fileName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.validationError(
                List.of(new FieldError("file", "INVALID_FILENAME", "File must have a valid name"))
            ));
        }

        return Result.success(file);
    }

    /**
     * Extracts multipart data from a ServerRequest with error handling.
     */
    public Result<org.springframework.util.MultiValueMap<String, jakarta.servlet.http.Part>> extractMultipartData(
            org.springframework.web.servlet.function.ServerRequest request) {

        try {
            return Result.success(request.multipartData());
        } catch (IOException | jakarta.servlet.ServletException e) {
            log.error("Failed to extract multipart data: {}", e.getMessage(), e);
            return Result.failure(ErrorDetail.of("MULTIPART_EXTRACTION_ERROR", ErrorType.SYSTEM_ERROR,
                "Failed to process multipart request: " + e.getMessage(), "multipart.extraction.error"));
        }
    }
}