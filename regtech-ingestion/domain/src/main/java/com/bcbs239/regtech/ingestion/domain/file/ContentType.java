package com.bcbs239.regtech.ingestion.domain.file;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.Set;

/**
 * ContentType Value Object with validation
 */
public record ContentType(String value) {

    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
        "application/json",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    public static Result<ContentType> create(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_CONTENT_TYPE", ErrorType.SYSTEM_ERROR, "Content type cannot be null or empty", "generic.error"));
        }

        String trimmedContentType = contentType.trim();
        if (!SUPPORTED_CONTENT_TYPES.contains(trimmedContentType)) {
            return Result.failure(ErrorDetail.of("UNSUPPORTED_CONTENT_TYPE", ErrorType.VALIDATION_ERROR,
                String.format("Content type '%s' is not supported. Supported types: %s",
                    trimmedContentType, SUPPORTED_CONTENT_TYPES), "file.unsupported.content.type"));
        }

        return Result.success(new ContentType(trimmedContentType));
    }

    public boolean isJson() {
        return "application/json".equals(value);
    }

    public boolean isExcel() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(value);
    }

    public String getValue() {
        return value;
    }
}