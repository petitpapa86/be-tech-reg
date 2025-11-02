package com.bcbs239.regtech.ingestion.infrastructure.service;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Service for validating file upload constraints including size and content type.
 * Validates files before processing to ensure they meet system requirements.
 */
@Service
@Slf4j
public class FileUploadValidationService {

    private static final long MAX_FILE_SIZE_BYTES = 500L * 1024 * 1024; // 500MB
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
        "application/json",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // Excel .xlsx
        "application/vnd.ms-excel" // Excel .xls (legacy support)
    );

    /**
     * Validate file upload constraints
     */
    public Result<FileUploadValidationResult> validateFileUpload(MultipartFile file) {
        log.info("Validating file upload: {} (size: {} bytes, content-type: {})", 
            file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        // Check if file is empty
        if (file.isEmpty()) {
            return Result.failure(ErrorDetail.of("EMPTY_FILE", 
                "Uploaded file is empty. Please select a valid file."));
        }
        
        // Validate file size
        Result<Void> sizeValidation = validateFileSize(file);
        if (sizeValidation.isFailure()) {
            return Result.failure(sizeValidation.getError().orElseThrow());
        }
        
        // Validate content type
        Result<Void> contentTypeValidation = validateContentType(file);
        if (contentTypeValidation.isFailure()) {
            return Result.failure(contentTypeValidation.getError().orElseThrow());
        }
        
        // Validate file name
        Result<Void> fileNameValidation = validateFileName(file);
        if (fileNameValidation.isFailure()) {
            return Result.failure(fileNameValidation.getError().orElseThrow());
        }
        
        FileUploadValidationResult result = FileUploadValidationResult.builder()
            .valid(true)
            .fileName(file.getOriginalFilename())
            .contentType(file.getContentType())
            .fileSizeBytes(file.getSize())
            .isJson(isJsonFile(file.getContentType()))
            .isExcel(isExcelFile(file.getContentType()))
            .build();
        
        log.info("File upload validation passed for: {}", file.getOriginalFilename());
        return Result.success(result);
    }

    /**
     * Validate file size is within limits
     */
    private Result<Void> validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            long fileSizeMB = file.getSize() / (1024 * 1024);
            long maxSizeMB = MAX_FILE_SIZE_BYTES / (1024 * 1024);
            
            return Result.failure(ErrorDetail.of("FILE_TOO_LARGE", 
                String.format("File size (%d MB) exceeds maximum allowed size (%d MB). " +
                    "Please split your file into smaller chunks or compress the data.", 
                    fileSizeMB, maxSizeMB)));
        }
        
        if (file.getSize() == 0) {
            return Result.failure(ErrorDetail.of("EMPTY_FILE", 
                "File appears to be empty (0 bytes). Please check your file and try again."));
        }
        
        return Result.success(null);
    }

    /**
     * Validate content type is supported
     */
    private Result<Void> validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        
        if (contentType == null || contentType.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_CONTENT_TYPE", 
                "File content type could not be determined. Please ensure you're uploading a valid JSON or Excel file."));
        }
        
        // Normalize content type (remove charset and other parameters)
        String normalizedContentType = contentType.split(";")[0].trim().toLowerCase();
        
        if (!SUPPORTED_CONTENT_TYPES.contains(normalizedContentType)) {
            return Result.failure(ErrorDetail.of("UNSUPPORTED_CONTENT_TYPE", 
                String.format("Content type '%s' is not supported. " +
                    "Please upload a JSON file (.json) or Excel file (.xlsx).", contentType)));
        }
        
        return Result.success(null);
    }

    /**
     * Validate file name is reasonable
     */
    private Result<Void> validateFileName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        
        if (fileName == null || fileName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("MISSING_FILE_NAME", 
                "File name is missing. Please ensure your file has a valid name."));
        }
        
        // Check for reasonable file name length
        if (fileName.length() > 255) {
            return Result.failure(ErrorDetail.of("FILE_NAME_TOO_LONG", 
                "File name is too long (max 255 characters). Please rename your file."));
        }
        
        // Check for potentially dangerous characters
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_NAME", 
                "File name contains invalid characters. Please use only alphanumeric characters, spaces, hyphens, and underscores."));
        }
        
        // Validate file extension matches content type
        String lowerFileName = fileName.toLowerCase();
        String contentType = file.getContentType();
        
        if (contentType != null) {
            String normalizedContentType = contentType.split(";")[0].trim().toLowerCase();
            
            if (normalizedContentType.equals("application/json") && !lowerFileName.endsWith(".json")) {
                return Result.failure(ErrorDetail.of("FILE_EXTENSION_MISMATCH", 
                    "File has JSON content type but doesn't have .json extension."));
            }
            
            if ((normalizedContentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                 normalizedContentType.equals("application/vnd.ms-excel")) && 
                !lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
                return Result.failure(ErrorDetail.of("FILE_EXTENSION_MISMATCH", 
                    "File has Excel content type but doesn't have .xlsx or .xls extension."));
            }
        }
        
        return Result.success(null);
    }

    /**
     * Check if file is JSON based on content type
     */
    private boolean isJsonFile(String contentType) {
        if (contentType == null) return false;
        String normalizedContentType = contentType.split(";")[0].trim().toLowerCase();
        return "application/json".equals(normalizedContentType);
    }

    /**
     * Check if file is Excel based on content type
     */
    private boolean isExcelFile(String contentType) {
        if (contentType == null) return false;
        String normalizedContentType = contentType.split(";")[0].trim().toLowerCase();
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(normalizedContentType) ||
               "application/vnd.ms-excel".equals(normalizedContentType);
    }

    /**
     * Get maximum allowed file size in bytes
     */
    public long getMaxFileSizeBytes() {
        return MAX_FILE_SIZE_BYTES;
    }

    /**
     * Get supported content types
     */
    public Set<String> getSupportedContentTypes() {
        return SUPPORTED_CONTENT_TYPES;
    }

    /**
     * Format file size for human-readable display
     */
    public String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}