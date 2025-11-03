package com.bcbs239.regtech.modules.ingestion.domain.batch;

import java.util.Objects;
import java.util.Set;

/**
 * Value object representing file metadata with validation logic.
 */
public record FileMetadata(
    String fileName,
    String contentType,
    long fileSizeBytes,
    String md5Checksum,
    String sha256Checksum
) {
    
    private static final long MAX_FILE_SIZE_BYTES = 500L * 1024 * 1024; // 500MB
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
        "application/json",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    
    public FileMetadata {
        Objects.requireNonNull(fileName, "File name cannot be null");
        Objects.requireNonNull(contentType, "Content type cannot be null");
        Objects.requireNonNull(md5Checksum, "MD5 checksum cannot be null");
        // sha256Checksum can be null as it's calculated during S3 storage
        
        if (fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
        if (md5Checksum.trim().isEmpty()) {
            throw new IllegalArgumentException("MD5 checksum cannot be empty");
        }
        if (sha256Checksum != null && sha256Checksum.trim().isEmpty()) {
            throw new IllegalArgumentException("SHA-256 checksum cannot be empty if provided");
        }
    }
    
    /**
     * Validates if the file metadata meets all requirements.
     */
    public boolean isValid() {
        return fileSizeBytes > 0 
            && fileSizeBytes <= MAX_FILE_SIZE_BYTES
            && SUPPORTED_CONTENT_TYPES.contains(contentType);
    }
    
    /**
     * Checks if the file size is within the allowed limit.
     */
    public boolean isWithinSizeLimit() {
        return fileSizeBytes <= MAX_FILE_SIZE_BYTES;
    }
    
    /**
     * Checks if the content type is supported.
     */
    public boolean isSupportedContentType() {
        return SUPPORTED_CONTENT_TYPES.contains(contentType);
    }
    
    /**
     * Returns the maximum allowed file size in bytes.
     */
    public static long getMaxFileSizeBytes() {
        return MAX_FILE_SIZE_BYTES;
    }
    
    /**
     * Returns the set of supported content types.
     */
    public static Set<String> getSupportedContentTypes() {
        return Set.copyOf(SUPPORTED_CONTENT_TYPES);
    }
}