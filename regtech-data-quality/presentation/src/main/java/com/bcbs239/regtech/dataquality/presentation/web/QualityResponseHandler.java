package com.bcbs239.regtech.dataquality.presentation.web;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Handler for converting application results to HTTP responses.
 * Provides consistent response formatting for quality report endpoints.
 */
@Component
public class QualityResponseHandler {

    /**
     * Converts a Result to a ServerResponse with success handling.
     */
    public <T> ServerResponse handleSuccessResult(Result<T> result, String successMessage, String messageKey) {
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
     * Converts an ErrorDetail to a ServerResponse.
     */
    public ServerResponse handleErrorResponse(ErrorDetail error) {
        if (error != null) {
            return ServerResponse.status(getHttpStatus(error))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(error));
        }
        return ServerResponse.status(500)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorResponse("Unknown error", "system.error"));
    }

    /**
     * Converts an Exception to a ServerResponse.
     */
    public ServerResponse handleSystemErrorResponse(Exception e) {
        return ServerResponse.status(500)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new ErrorResponse(e.getMessage(), "system.error"));
    }

    private int getHttpStatus(ErrorDetail error) {
        // Map error types to HTTP status codes
        return switch (error.getErrorType()) {
            case VALIDATION_ERROR -> 400;
            case NOT_FOUND_ERROR -> 404;
            default -> 500;
        };
    }

    // Simple response DTOs
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
}

