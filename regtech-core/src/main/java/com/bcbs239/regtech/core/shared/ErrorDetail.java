package com.bcbs239.regtech.core.shared;

public class ErrorDetail {

    private final String code;
    private final String message;
    private final String details;

    public ErrorDetail(String code, String message, String details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public ErrorDetail(String code, String message) {
        this(code, message, null);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }
}