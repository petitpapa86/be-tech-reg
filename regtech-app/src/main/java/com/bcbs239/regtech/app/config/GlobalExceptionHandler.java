package com.bcbs239.regtech.app.config;


import com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the RegTech application.
 * This class handles all unhandled exceptions thrown from controllers
 * and logs them for debugging purposes.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Logs error using both standard and structured logging
     */
    private void logError(String eventType, Exception ex, HttpServletRequest request, Map<String, Object> additionalContext) {
        String path = request.getRequestURI();

        // Standard logging
       // logger.error("Error [{}] at {}: {}", eventType, path, ex.getMessage(), ex);

        // Structured logging with full context
        Map<String, Object> context = new HashMap<>(Map.of(
                "eventType", eventType,
                "exception", ex.getClass().getSimpleName(),
                "message", ex.getMessage(),
                "path", path,
                "method", request.getMethod(),
                "correlationId", LoggingConfiguration.getCurrentCorrelationId()
        ));

        if (additionalContext != null) {
            context.putAll(additionalContext);
        }

        LoggingConfiguration.logError(eventType, ex, context);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        // Log as a not-found event rather than an internal server error
        logError("RESOURCE_NOT_FOUND", ex, request, Map.of(
                "reason", ex.getMessage()
        ));

        // Return a 404 with a structured API response
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ResponseUtils.notFoundError(ex.getMessage())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex, HttpServletRequest request) {
        logError("UNHANDLED_EXCEPTION", ex, request, null);

        return ResponseEntity.internalServerError().body(
                ResponseUtils.systemError(ex.getMessage())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        logError("DATA_INTEGRITY_VIOLATION", ex, request, Map.of(
                "severity", "HIGH",
                "requiresInvestigation", true
        ));

        return ResponseEntity.internalServerError().body(
                ResponseUtils.systemError(ex.getMessage())
        );
    }


}
