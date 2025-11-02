package com.bcbs239.regtech.ingestion.application.command.uploadfile;

import java.io.InputStream;

/**
 * Command for uploading a file for ingestion processing.
 * Contains file data and authentication information.
 */
public record UploadFileCommand(
    InputStream fileStream,
    String fileName,
    String contentType,
    long fileSizeBytes,
    String authToken
) {
    
    public UploadFileCommand {
        if (fileStream == null) {
            throw new IllegalArgumentException("File stream cannot be null");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
        if (authToken == null || authToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth token cannot be null or empty");
        }
    }
}