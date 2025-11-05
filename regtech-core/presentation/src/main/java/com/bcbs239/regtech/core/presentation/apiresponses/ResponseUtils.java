package com.bcbs239.regtech.core.presentation.apiresponses;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for creating API responses with common meta information.
 * Provides helpers for consistent response creation across all bounded contexts.
 */
public class ResponseUtils {

    private static final String VERSION = "1.0";
    private static final String API_VERSION = "v1";

    /**
     * Creates meta information with timestamp and version
     */
    public static Map<String, Object> createMeta() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("timestamp", Instant.now().toString());
        meta.put("version", VERSION);
        meta.put("apiVersion", API_VERSION);
        return meta;
    }

    /**
     * Creates meta information with additional custom data
     */
    public static Map<String, Object> createMeta(Map<String, Object> additionalMeta) {
        Map<String, Object> meta = createMeta();
        if (additionalMeta != null) {
            meta.putAll(additionalMeta);
        }
        return meta;
    }

    /**
     * Creates a success response with default meta
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>success()
                .data(data)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a success response with message and default meta
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>success()
                .data(data)
                .message(message)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a success response with message key and default meta
     */
    public static <T> ApiResponse<T> successWithKey(T data, String message, String messageKey) {
        return ApiResponse.<T>success()
                .data(data)
                .message(message)
                .messageKey(messageKey)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a validation error response with default meta
     */
    public static ApiResponse<Void> validationError(List<FieldError> errors) {
        return ApiResponse.error()
                .type(ErrorType.VALIDATION_ERROR)
                .errors(errors)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a validation error response with message and default meta
     */
    public static ApiResponse<Void> validationError(List<FieldError> errors, String message) {
        return ApiResponse.error()
                .type(ErrorType.VALIDATION_ERROR)
                .message(message)
                .errors(errors)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a business rule error response with default meta
     */
    public static ApiResponse<Void> businessRuleError(String message) {
        return ApiResponse.error()
                .type(ErrorType.BUSINESS_RULE_ERROR)
                .message(message)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a business rule error response with message key and default meta
     */
    public static ApiResponse<Void> businessRuleError(String message, String messageKey) {
        return ApiResponse.error()
                .type(ErrorType.BUSINESS_RULE_ERROR)
                .message(message)
                .messageKey(messageKey)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a system error response with default meta
     */
    public static ApiResponse<Void> systemError(String message) {
        return ApiResponse.error()
                .type(ErrorType.SYSTEM_ERROR)
                .message(message)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates an authentication error response with default meta
     */
    public static ApiResponse<Void> authenticationError(String message) {
        return ApiResponse.error()
                .type(ErrorType.AUTHENTICATION_ERROR)
                .message(message)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a not found error response with default meta
     */
    public static ApiResponse<Void> notFoundError(String message) {
        return ApiResponse.error()
                .type(ErrorType.NOT_FOUND_ERROR)
                .message(message)
                .meta(createMeta())
                .build();
    }

    /**
     * Creates a field error for validation responses
     */
    public static FieldError fieldError(String field, String code, String message) {
        return new FieldError(field, code, message);
    }

    /**
     * Creates a field error with message key for validation responses
     */
    public static FieldError fieldError(String field, String code, String message, String messageKey) {
        return new FieldError(field, code, message, messageKey);
    }
}

