package com.bcbs239.regtech.core.application.shared;

public class FieldError {
    private final String field;
    private final String message;
    private final Object rejectedValue;

    public FieldError(String field, String message) {
        this(field, message, null);
    }

    public FieldError(String field, String message, Object rejectedValue) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }
}