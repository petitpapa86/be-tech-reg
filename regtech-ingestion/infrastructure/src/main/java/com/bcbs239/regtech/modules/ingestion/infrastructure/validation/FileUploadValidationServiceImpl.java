package com.bcbs239.regtech.modules.ingestion.infrastructure.validation;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.ingestion.application.batch.upload.UploadFileCommandHandler.FileUploadValidationService;
import com.bcbs239.regtech.modules.ingestion.domain.batch.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Implementation of file upload validation service.
 * Validates file size, content type, and other upload constraints.
 */
@Service
public class FileUploadValidationServiceImpl implements FileUploadValidationService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadValidationServiceImpl.class);
    
    @Value("${ingestion.file.max-size-bytes:524288000}") // 500MB default
    private long maxFileSizeBytes;

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
        "application/json",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Override
    public Result<Void> validateUpload(String fileName, String contentType, long fileSizeBytes) {
        log.debug("Validating file upload: {} (type: {}, size: {} bytes)", 
            fileName, contentType, fileSizeBytes);

        // Validate file name
        if (fileName == null || fileName.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_FILE_NAME", 
                "File name cannot be null or empty"));
        }

        // Validate content type
        if (contentType == null || contentType.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_CONTENT_TYPE", 
                "Content type cannot be null or empty"));
        }

        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            return Result.failure(new ErrorDetail("UNSUPPORTED_CONTENT_TYPE", 
                String.format("Content type '%s' is not supported. Supported types: %s", 
                    contentType, SUPPORTED_CONTENT_TYPES)));
        }

        // Validate file size
        if (fileSizeBytes <= 0) {
            return Result.failure(new ErrorDetail("INVALID_FILE_SIZE", 
                "File size must be positive"));
        }

        if (fileSizeBytes > maxFileSizeBytes) {
            return Result.failure(new ErrorDetail("FILE_TOO_LARGE", 
                String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                    fileSizeBytes, maxFileSizeBytes)));
        }

        // Additional validations based on content type
        if ("application/json".equals(contentType)) {
            return validateJsonFile(fileName, fileSizeBytes);
        } else if ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType)) {
            return validateExcelFile(fileName, fileSizeBytes);
        }

        log.debug("File upload validation passed for: {}", fileName);
        return Result.success(null);
    }

    private Result<Void> validateJsonFile(String fileName, long fileSizeBytes) {
        // Validate JSON file extension
        if (!fileName.toLowerCase().endsWith(".json")) {
            return Result.failure(new ErrorDetail("INVALID_FILE_EXTENSION", 
                "JSON files must have .json extension"));
        }

        // JSON files should be reasonably sized for parsing
        if (fileSizeBytes > 100 * 1024 * 1024) { // 100MB for JSON
            log.warn("Large JSON file detected: {} ({} bytes)", fileName, fileSizeBytes);
        }

        return Result.success(null);
    }

    private Result<Void> validateExcelFile(String fileName, long fileSizeBytes) {
        // Validate Excel file extension
        String lowerFileName = fileName.toLowerCase();
        if (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
            return Result.failure(new ErrorDetail("INVALID_FILE_EXTENSION", 
                "Excel files must have .xlsx or .xls extension"));
        }

        // Excel files can be larger due to formatting overhead
        if (fileSizeBytes > 200 * 1024 * 1024) { // 200MB for Excel
            log.warn("Large Excel file detected: {} ({} bytes)", fileName, fileSizeBytes);
        }

        return Result.success(null);
    }
}