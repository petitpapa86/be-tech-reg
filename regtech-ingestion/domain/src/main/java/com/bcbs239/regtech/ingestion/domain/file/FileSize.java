package com.bcbs239.regtech.ingestion.domain.file;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * FileSize Value Object with validation
 */
public record FileSize(long bytes) {

    private static final long MAX_FILE_SIZE_BYTES = 524288000L; // 500MB default
    private static final long JSON_SIZE_WARNING_BYTES = 100L * 1024 * 1024; // 100MB for JSON
    private static final long EXCEL_SIZE_WARNING_BYTES = 200L * 1024 * 1024; // 200MB for Excel

    public static Result<FileSize> create(long fileSizeBytes) {
        if (fileSizeBytes <= 0) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_SIZE", ErrorType.SYSTEM_ERROR, "File size must be positive", "generic.error"));
        }

        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            return Result.failure(ErrorDetail.of("FILE_TOO_LARGE", ErrorType.VALIDATION_ERROR,
                String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    fileSizeBytes, MAX_FILE_SIZE_BYTES), "file.size.exceeded"));
        }

        return Result.success(new FileSize(fileSizeBytes));
    }

    public boolean shouldWarnForJson() {
        return bytes > JSON_SIZE_WARNING_BYTES;
    }

    public boolean shouldWarnForExcel() {
        return bytes > EXCEL_SIZE_WARNING_BYTES;
    }

    public long getBytes() {
        return bytes;
    }
}