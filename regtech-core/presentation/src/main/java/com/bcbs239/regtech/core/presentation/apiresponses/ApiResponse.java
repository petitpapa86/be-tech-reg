package com.bcbs239.regtech.core.presentation.apiresponses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Unified API response envelope for both success and error responses.
 * Provides consistent structure across all bounded contexts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @JsonProperty("success")
    private final boolean success;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("messageKey")
    private final String messageKey;

    @JsonProperty("data")
    private final T data;

    @JsonProperty("type")
    private final ErrorType type;

    @JsonProperty("errors")
    private final List<FieldError> errors;

    @JsonProperty("meta")
    private final Map<String, Object> meta;

    private ApiResponse(boolean success, String message, String messageKey, T data,
                       ErrorType type, List<FieldError> errors, Map<String, Object> meta) {
        this.success = success;
        this.message = message;
        this.messageKey = messageKey;
        this.data = data;
        this.type = type;
        this.errors = errors;
        this.meta = meta;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public T getData() {
        return data;
    }

    public ErrorType getType() {
        return type;
    }

    public List<FieldError> getErrors() {
        return errors;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    /**
     * Builder for creating success responses
     */
    public static class SuccessBuilder<T> {
        private String message;
        private String messageKey;
        private T data;
        private Map<String, Object> meta;

        public SuccessBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public SuccessBuilder<T> messageKey(String messageKey) {
            this.messageKey = messageKey;
            return this;
        }

        public SuccessBuilder<T> data(T data) {
            this.data = data;
            return this;
        }

        public SuccessBuilder<T> meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(true, message, messageKey, data, null, null, meta);
        }
    }

    /**
     * Builder for creating error responses
     */
    public static class ErrorBuilder {
        private ErrorType type;
        private String message;
        private String messageKey;
        private List<FieldError> errors;
        private Map<String, Object> meta;

        public ErrorBuilder type(ErrorType type) {
            this.type = type;
            return this;
        }

        public ErrorBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorBuilder messageKey(String messageKey) {
            this.messageKey = messageKey;
            return this;
        }

        public ErrorBuilder errors(List<FieldError> errors) {
            this.errors = errors;
            return this;
        }

        public ErrorBuilder meta(Map<String, Object> meta) {
            this.meta = meta;
            return this;
        }

        public ApiResponse<Void> build() {
            return new ApiResponse<>(false, message, messageKey, null, type, errors, meta);
        }
    }

    /**
     * Static factory methods for creating responses
     */
    public static <T> SuccessBuilder<T> success() {
        return new SuccessBuilder<>();
    }

    public static ErrorBuilder error() {
        return new ErrorBuilder();
    }

    /**
     * Convenience methods for common responses
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>success().data(data).build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>success().data(data).message(message).build();
    }

    public static ApiResponse<Void> validationError(List<FieldError> errors) {
        return ApiResponse.error()
                .type(ErrorType.VALIDATION_ERROR)
                .errors(errors)
                .build();
    }

    public static ApiResponse<Void> validationError(List<FieldError> errors, String message) {
        return ApiResponse.error()
                .type(ErrorType.VALIDATION_ERROR)
                .message(message)
                .errors(errors)
                .build();
    }

    public static ApiResponse<Void> businessRuleError(String message) {
        return ApiResponse.error()
                .type(ErrorType.BUSINESS_RULE_ERROR)
                .message(message)
                .build();
    }

    public static ApiResponse<Void> businessRuleError(String message, String messageKey) {
        return ApiResponse.error()
                .type(ErrorType.BUSINESS_RULE_ERROR)
                .message(message)
                .messageKey(messageKey)
                .build();
    }

    public static ApiResponse<Void> systemError(String message) {
        return ApiResponse.error()
                .type(ErrorType.SYSTEM_ERROR)
                .message(message)
                .build();
    }

    public static ApiResponse<Void> authenticationError(String message) {
        return ApiResponse.error()
                .type(ErrorType.AUTHENTICATION_ERROR)
                .message(message)
                .build();
    }

    public static ApiResponse<Void> notFoundError(String message) {
        return ApiResponse.error()
                .type(ErrorType.NOT_FOUND_ERROR)
                .message(message)
                .build();
    }
}
