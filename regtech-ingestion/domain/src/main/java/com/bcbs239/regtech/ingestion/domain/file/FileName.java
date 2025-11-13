package com.bcbs239.regtech.ingestion.domain.file;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * FileName Value Object with validation
 */
public record FileName(String value) {

    public static Result<FileName> create(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_NAME", ErrorType.SYSTEM_ERROR, "File name cannot be null or empty", "generic.error"));
        }

        return Result.success(new FileName(fileName.trim()));
    }

    public boolean hasJsonExtension() {
        return value.toLowerCase().endsWith(".json");
    }

    public boolean hasExcelExtension() {
        String lowerValue = value.toLowerCase();
        return lowerValue.endsWith(".xlsx") || lowerValue.endsWith(".xls");
    }

    public String getValue() {
        return value;
    }
}