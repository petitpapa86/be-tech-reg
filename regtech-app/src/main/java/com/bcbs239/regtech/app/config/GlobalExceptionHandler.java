package com.bcbs239.regtech.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

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
     * Handles all unhandled exceptions.
     * Logs the exception and returns a generic error response.
     *
     * @param ex the exception
     * @param request the web request
     * @return a ResponseEntity with error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception occurred: ", ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("message", "An unexpected error occurred.");
        errorDetails.put("error", ex.getClass().getSimpleName());
        errorDetails.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}