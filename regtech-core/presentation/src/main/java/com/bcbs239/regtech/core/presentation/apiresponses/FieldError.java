package com.bcbs239.regtech.core.presentation.apiresponses;

public class FieldError {
    private final String field;
    private final String message;
    private final Object rejectedValue;
    private final String messageKey;

    public FieldError(String field, String message) {
        this(field, message, null, null);
    }

    public FieldError(String field, String message, Object rejectedValue) {
        this(field, message, rejectedValue, null);
    }

    public FieldError(String field, String message, Object rejectedValue, String messageKey) {
        this.field = field;
        this.message = message;
        this.rejectedValue = rejectedValue;
        this.messageKey = messageKey;
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

    public String getMessageKey() {
        return messageKey;
    }
}