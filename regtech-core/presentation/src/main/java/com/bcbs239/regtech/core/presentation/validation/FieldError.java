package com.bcbs239.regtech.core.presentation.validation;

/**
 * Represents a field-level validation error.
 * Used in VALIDATION_ERROR responses to specify which field has the error.
 */
public class FieldError {

    private final String field;
    private final String code;
    private final String message;
    private final Object rejectedValue;
    private final String messageKey;

    public FieldError(String field, String code, String message, String messageKey) {
        this(field, code, message, null, messageKey);
    }

    public FieldError(String field, String code, String message) {
        this(field, code, message, null, null);
    }

    public FieldError(String field, String code, String message, Object rejectedValue, String messageKey) {
        this.field = field;
        this.code = code;
        this.message = message;
        this.rejectedValue = rejectedValue;
        this.messageKey = messageKey;
    }

    public String getField() {
        return field;
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

    public Object getRejectedValue() {
        return rejectedValue;
    }

    @Override
    public String toString() {
        return "FieldError{" +
                "field='" + field + '\'' +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", messageKey='" + messageKey + '\'' +
                '}';
    }
}

