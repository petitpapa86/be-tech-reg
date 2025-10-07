package com.bcbs239.regtech.core.shared;

import java.util.List;

public class ErrorDetail {

    private final String code;
    private final String message;
    private final String messageKey;
    private final String details;
    private final List<FieldError> fieldErrors;

    public ErrorDetail(String code, String message, String messageKey, String details, List<FieldError> fieldErrors) {
        this.code = code;
        this.message = message;
        this.messageKey = messageKey;
        this.details = details;
        this.fieldErrors = fieldErrors;
    }

    public ErrorDetail(String code, String message, String messageKey, String details) {
        this(code, message, messageKey, details, null);
    }

    public ErrorDetail(String code, String message, String messageKey) {
        this(code, message, messageKey, null, null);
    }

    public ErrorDetail(String code, String message) {
        this(code, message, null, null, null);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getDetails() {
        return details;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }

    // Static factory methods
    public static ErrorDetail of(String code, String message, String messageKey) {
        return new ErrorDetail(code, message, messageKey);
    }

    public static ErrorDetail validationError(List<FieldError> fieldErrors) {
        return new ErrorDetail("VALIDATION_ERROR", "Validation failed", "error.validation", null, fieldErrors);
    }

    public static ErrorDetail of(String code) {
        return new ErrorDetail(code, null, code.toLowerCase().replace("_", "."));
    }
}