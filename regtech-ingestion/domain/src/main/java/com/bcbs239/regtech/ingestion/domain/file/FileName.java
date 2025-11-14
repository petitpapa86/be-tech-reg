package com.bcbs239.regtech.ingestion.domain.file;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.regex.Pattern;

/**
 * FileName Value Object with validation.
 * Ensures file names meet security and business requirements.
 */
public record FileName(String value) {
    
    private static final Pattern VALID_FILE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$");
    private static final int MAX_LENGTH = 255;

    public static Result<FileName> create(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_NAME", ErrorType.VALIDATION_ERROR, "File name cannot be null or empty", "file.name.empty"));
        }
        
        String trimmed = fileName.trim();
        
        if (trimmed.length() > MAX_LENGTH) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_NAME", ErrorType.VALIDATION_ERROR, "File name exceeds maximum length of " + MAX_LENGTH, "file.name.too.long"));
        }
        
        if (!VALID_FILE_NAME_PATTERN.matcher(trimmed).matches()) {
            return Result.failure(ErrorDetail.of("INVALID_FILE_NAME", ErrorType.VALIDATION_ERROR, "File name contains invalid characters: " + trimmed, "file.name.invalid.characters"));
        }

        return Result.success(new FileName(trimmed));
    }

    public boolean hasJsonExtension() {
        return value.toLowerCase().endsWith(".json");
    }

    public boolean hasExcelExtension() {
        String lowerValue = value.toLowerCase();
        return lowerValue.endsWith(".xlsx") || lowerValue.endsWith(".xls");
    }
    
    public boolean hasCsvExtension() {
        return value.toLowerCase().endsWith(".csv");
    }
    
    /**
     * Get file extension if present.
     */
    public String getExtension() {
        int lastDot = value.lastIndexOf('.');
        if (lastDot > 0 && lastDot < value.length() - 1) {
            return value.substring(lastDot + 1);
        }
        return "";
    }
    
    /**
     * Get file name without extension.
     */
    public String getNameWithoutExtension() {
        int lastDot = value.lastIndexOf('.');
        if (lastDot > 0) {
            return value.substring(0, lastDot);
        }
        return value;
    }

    public String getValue() {
        return value;
    }
}