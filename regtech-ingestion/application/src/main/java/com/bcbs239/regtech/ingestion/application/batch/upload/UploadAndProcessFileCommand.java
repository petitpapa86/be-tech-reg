package com.bcbs239.regtech.ingestion.application.batch.upload;

import java.io.InputStream;

import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;
import com.bcbs239.regtech.ingestion.domain.file.FileName;


public record UploadAndProcessFileCommand(
    InputStream fileStream,
    FileName fileName,
    String contentType,
    long fileSizeBytes,
    String bankId,
    String batchId
) {

    public UploadAndProcessFileCommand {
        if (fileStream == null) {
            throw new IllegalArgumentException("File stream cannot be null");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("FileName value object must be provided and non-null");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
        if (bankId == null || bankId.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank ID cannot be null");
        }
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("Batch ID cannot be null");
        }
    }
}