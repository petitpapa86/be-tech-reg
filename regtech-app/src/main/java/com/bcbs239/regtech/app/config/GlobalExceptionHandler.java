package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

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
     * Logs the exception and returns a generic error response using existing ApiResponse format.
     *
     * @param ex the exception
     * @param request the web request
     * @return a ResponseEntity with structured error details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        
        LoggingConfiguration.logStructured("UNHANDLED_EXCEPTION", Map.of(
            "exception", ex.getClass().getSimpleName(),
            "message", ex.getMessage(),
            "path", path
        ));

        String message = "An unexpected error occurred. Please try again or contact support if the problem persists.";
        
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError(message)
        );
    }
}