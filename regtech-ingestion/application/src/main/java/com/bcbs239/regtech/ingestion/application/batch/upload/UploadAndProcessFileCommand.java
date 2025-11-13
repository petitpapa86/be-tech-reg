package com.bcbs239.regtech.ingestion.application.batch.upload;

import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import java.io.InputStream;

/**
 * Command for uploading and immediately processing a file for ingestion.
 * Combines the upload and process operations into a single transaction.
 */
public record UploadAndProcessFileCommand(
    InputStream fileStream,
    String fileName,
    String contentType,
    long fileSizeBytes,
    BankId bankId
) {

    public UploadAndProcessFileCommand {
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
        if (bankId == null) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
    }
}