package com.bcbs239.regtech.core.saga;

/**
 * Result of a saga operation, indicating success or failure with optional error details.
 */
public class SagaResult {

    private final boolean success;
    private final String errorMessage;
    private final Object data;

    private SagaResult(boolean success, String errorMessage, Object data) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public static SagaResult success() {
        return new SagaResult(true, null, null);
    }

    public static SagaResult success(Object data) {
        return new SagaResult(true, null, data);
    }

    public static SagaResult failure(String errorMessage) {
        return new SagaResult(false, errorMessage, null);
    }

    public static SagaResult failure(String errorMessage, Object data) {
        return new SagaResult(false, errorMessage, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return String.format("SagaResult{success=%s, errorMessage='%s'}", success, errorMessage);
    }
}