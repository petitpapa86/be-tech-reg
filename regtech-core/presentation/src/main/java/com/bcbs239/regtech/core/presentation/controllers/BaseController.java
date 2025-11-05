package com.bcbs239.regtech.core.presentation.controllers;

import com.bcbs239.regtech.core.domain.ErrorDetail;
import com.bcbs239.regtech.core.domain.FieldError;
import com.bcbs239.regtech.core.domain.Result;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Abstract base controller providing common response handling patterns
 * for all controllers across the application.
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

    /**
     * Handles ErrorDetail with appropriate HTTP status and response type.
     *
     * @param error The error detail
     * @return ResponseEntity with error ApiResponse
     */
    protected ResponseEntity<? extends ApiResponse<?>> handleError(ErrorDetail error) {
        // Determine error type based on error code and content
        if (error.hasFieldErrors() || isValidationError(error.getCode())) {
            return ResponseEntity.badRequest().body(
                ResponseUtils.validationError(error.getFieldErrors(), error.getMessage())
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

        return ResponseEntity.badRequest().body(
            ResponseUtils.validationError(fieldErrors, message)
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
            errorCode.equals("UNAUTHORIZED") ||
            errorCode.equals("INVALID_CREDENTIALS")
        );
    }

    private boolean isNotFoundError(String errorCode) {
        return errorCode != null && (
            errorCode.equals("USER_NOT_FOUND") ||
            errorCode.equals("RESOURCE_NOT_FOUND")
        );
    }
}
