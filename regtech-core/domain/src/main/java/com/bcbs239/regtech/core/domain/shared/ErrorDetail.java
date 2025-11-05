package com.bcbs239.regtech.core.domain.shared;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ErrorDetail {
    private final String code;
    private final String message;
    private final Map<String, Object> details;
    private final List<FieldError> fieldErrors;

    private ErrorDetail(String code, String message, Map<String, Object> details, List<FieldError> fieldErrors) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.fieldErrors = fieldErrors;
    }

    public static ErrorDetail of(String code, String message) {
        return new ErrorDetail(code, message, null, null);
    }

    public static ErrorDetail validationError(List<FieldError> fieldErrors) {
        return new ErrorDetail("VALIDATION_ERROR", "Validation failed", null, fieldErrors);
    }

    public static ErrorDetail validationError(List<FieldError> fieldErrors, String message) {
        return new ErrorDetail("VALIDATION_ERROR", message, null, fieldErrors);
    }

    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }

    public String getMessageKey() {
        return code; // For now, use code as message key
    }
}

