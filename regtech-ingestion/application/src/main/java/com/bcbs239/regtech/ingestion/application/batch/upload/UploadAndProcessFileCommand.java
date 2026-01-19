package com.bcbs239.regtech.ingestion.application.batch.upload;

import java.io.InputStream;

import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.batch.BatchId;


public record UploadAndProcessFileCommand(
    InputStream fileStream,
    String fileName,
    String contentType,
    long fileSizeBytes,
    BankId bankId,
    BatchId batchId
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