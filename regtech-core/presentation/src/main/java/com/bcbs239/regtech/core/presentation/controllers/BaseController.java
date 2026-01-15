package com.bcbs239.regtech.core.presentation.controllers;


import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

/**
 * Abstract base controller providing common response handling patterns
 * for all controllers across the application.
 * 
 * <p>Supports both traditional ResponseEntity (REST controllers) and 
 * functional ServerResponse (functional endpoints) patterns.
 */
public abstract class BaseController {

    /**
     * Handles Result<T> responses from command handlers with consistent error categorization.
     *
     * @param result The result from a command handler
     * @param successMessageKey The message key for success responses
     * @param <T> The type of the success data
     * @return ResponseEntity with appropriate ApiResponse
     */
    protected <T> ResponseEntity<? extends ApiResponse<?>> handleResult(
            Result<T> result,
            String successMessageKey) {

        if (result.isSuccess()) {
            T data = result.getValue().get();
            return ResponseEntity.ok(
                ResponseUtils.successWithKey(data, "Operation completed successfully", successMessageKey)
            );
        } else {
            ErrorDetail error = result.getError().get();
            return handleError(error);
        }
    }

    /**
     * Handles Result<T> responses with custom success message.
     *
     * @param result The result from a command handler
     * @param successMessage The success message
     * @param successMessageKey The message key for success responses
     * @param <T> The type of the success data
     * @return ResponseEntity with appropriate ApiResponse
     */
    protected <T> ResponseEntity<? extends ApiResponse<?>> handleResult(
            Result<T> result,
            String successMessage,
            String successMessageKey) {

        if (result.isSuccess()) {
            T data = result.getValue().get();
            return ResponseEntity.ok(
                ResponseUtils.successWithKey(data, successMessage, successMessageKey)
            );
        } else {
            ErrorDetail error = result.getError().get();
            return handleError(error);
        }
    }

    // ========================================================================
    // Functional Endpoint Support (ServerResponse for functional routing)
    // ========================================================================

    /**
     * Converts a Result to a ServerResponse with success handling.
     * Suitable for functional endpoint routing.
     */
    protected <T> ServerResponse handleSuccessResult(Result<T> result, String successMessage, String messageKey) {
        if (result.isSuccess()) {
            T data = result.getValue().orElse(null);
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SuccessResponse<>(data, successMessage, messageKey));
        } else {
            ErrorDetail error = result.getError().orElse(null);
            return handleErrorResponse(error);
        }
    }

    /**
     * Converts an ErrorDetail to a ServerResponse with sophisticated error categorization.
     * Handles field validation errors, business rule errors, authentication errors, etc.
     * Suitable for functional endpoint routing.
     */
    protected ServerResponse handleErrorResponse(ErrorDetail error) {
        if (error == null) {
            return ServerResponse.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse("Unknown error", "system.error"));
        }

        // Handle field validation errors
        if (error.hasFieldErrors()) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ValidationErrorResponse(error));
        }
        
        // Handle validation errors without field errors
        if (isValidationError(error.getCode())) {
            return ServerResponse.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(error));
        }
        
        // Handle authentication errors
        if (isAuthenticationError(error.getCode())) {
            return ServerResponse.status(401)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(error));
        }
        
        // Handle not found errors
        if (isNotFoundError(error.getCode())) {
            return ServerResponse.status(404)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(error));
        }
        
        // Default to bad request for business rule violations
        return ServerResponse.badRequest()
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorResponse(error));
    }

    /**
     * Converts an Exception to a ServerResponse.
     * Suitable for functional endpoint routing.
     */
    protected ServerResponse handleSystemErrorResponse(Exception e) {
        return ServerResponse.status(500)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorResponse(e.getMessage(), "system.error"));
    }



    /**
     * Handles ErrorDetail with appropriate HTTP status and response type.
     *
     * @param error The error detail
     * @return ResponseEntity with error ApiResponse
     */
    protected ResponseEntity<? extends ApiResponse<?>> handleError(ErrorDetail error) {
        // Determine error type based on error code and content
        if (error.hasFieldErrors()) {
            List<FieldError> presentationErrors =
                error.getFieldErrors().stream()
                    .map(fe -> new FieldError(
                        fe.field(), fe.message(), fe.messageKey()))
                    .toList();
            return ResponseEntity.badRequest().body(
                ResponseUtils.validationError(presentationErrors, error.getMessage())
            );
        } else if (isValidationError(error.getCode())) {
            // Validation error without field errors - treat as business rule error
            return ResponseEntity.badRequest().body(
                ResponseUtils.businessRuleError(error.getMessage(), error.getMessageKey())
            );
        } else if (isAuthenticationError(error.getCode())) {
            return ResponseEntity.status(401).body(
                ResponseUtils.authenticationError(error.getMessage())
            );
        } else if (isNotFoundError(error.getCode())) {
            return ResponseEntity.status(404).body(
                ResponseUtils.notFoundError(error.getMessage())
            );
        } else {
            // Default to business rule error for domain/business logic violations
            return ResponseEntity.badRequest().body(
                ResponseUtils.businessRuleError(error.getMessage(), error.getMessageKey())
            );
        }
    }

    /**
     * Handles validation errors from command constructor or other sources.
     *
     * @param fieldErrors List of field errors
     * @param message Error message
     * @return ResponseEntity with validation error response
     */
    protected ResponseEntity<? extends ApiResponse<?>> handleValidationError(
            List<FieldError> fieldErrors,
            String message) {

        List<FieldError> presentationErrors =
            fieldErrors.stream()
                .map(fe -> new FieldError(
                    fe.field(), fe.message(), fe.messageKey()))
                .toList();

        return ResponseEntity.badRequest().body(
            ResponseUtils.validationError(presentationErrors, message)
        );
    }

    /**
     * Handles unexpected exceptions.
     *
     * @param e The exception
     * @return ResponseEntity with system error response
     */
    protected ResponseEntity<? extends ApiResponse<?>> handleSystemError(Exception e) {
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError("An unexpected error occurred: " + e.getMessage())
        );
    }

    /**
     * Helper methods for error type determination
     */
    private boolean isValidationError(String errorCode) {
        return errorCode != null && (
            errorCode.startsWith("VALIDATION_") ||
            errorCode.startsWith("EMAIL_") ||
            errorCode.startsWith("PASSWORD_") ||
            errorCode.equals("INVALID_FORMAT")
        );
    }

    private boolean isAuthenticationError(String errorCode) {
        return errorCode != null && (
            errorCode.startsWith("AUTH_") ||
            errorCode.startsWith("JWT_") ||
            errorCode.equals("UNAUTHORIZED") ||
            errorCode.equals("INVALID_CREDENTIALS") ||
            errorCode.equals("ACCOUNT_DISABLED") ||
            errorCode.equals("INVALID_REFRESH_TOKEN") ||
            errorCode.equals("BANK_ACCESS_DENIED") ||
            errorCode.equals("MISSING_TOKEN") ||
            errorCode.equals("AUTHENTICATION_ERROR")
        );
    }

    private boolean isNotFoundError(String errorCode) {
        return errorCode != null && (
            errorCode.equals("USER_NOT_FOUND") ||
            errorCode.equals("BANK_NOT_FOUND") ||
            errorCode.equals("RESOURCE_NOT_FOUND")
        );
    }

    // ========================================================================
    // Response DTOs for functional endpoints
    // ========================================================================

    /**
     * Success response wrapper for functional endpoints.
     */
    @Getter
    public static class SuccessResponse<T> {
        private final boolean success = true;
        private final T data;
        private final String message;
        private final String messageKey;

        public SuccessResponse(T data, String message, String messageKey) {
            this.data = data;
            this.message = message;
            this.messageKey = messageKey;
        }
    }

    /**
     * Error response wrapper for functional endpoints.
     */
    @Getter
    public static class ErrorResponse {
        private final boolean success = false;
        private final String error;
        private final String messageKey;

        public ErrorResponse(String error, String messageKey) {
            this.error = error;
            this.messageKey = messageKey;
        }

        public ErrorResponse(ErrorDetail errorDetail) {
            this.error = errorDetail.getMessage();
            this.messageKey = errorDetail.getMessageKey();
        }
    }

    /**
     * Validation error response wrapper for functional endpoints with field errors.
     */
    @Getter
    protected static class ValidationErrorResponse {
        private final boolean success = false;
        private final String error;
        private final String messageKey;
        private final List<FieldErrorDto> fieldErrors;

        public ValidationErrorResponse(ErrorDetail errorDetail) {
            this.error = errorDetail.getMessage();
            this.messageKey = errorDetail.getMessageKey();
            this.fieldErrors = errorDetail.getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.field(), fe.message(), fe.messageKey()))
                .toList();
        }
    }

    /**
     * Field error DTO for validation error responses.
     */
    @Getter
    protected static class FieldErrorDto {
        private final String field;
        private final String message;
        private final String messageKey;

        public FieldErrorDto(String field, String message, String messageKey) {
            this.field = field;
            this.message = message;
            this.messageKey = messageKey;
        }
    }
}
