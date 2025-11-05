package com.bcbs239.regtech.dataquality.presentation.web;

import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.FieldError;
import com.bcbs239.regtech.core.shared.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

/**
 * Global exception handler for the data quality module.
 * Handles module-specific exceptions and provides consistent error responses.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6
 */
@ControllerAdvice
public class QualityExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityExceptionHandler.class);
    
    /**
     * Handles IllegalArgumentException from domain objects and value objects.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Validation error: {}", e.getMessage());
        
        FieldError fieldError = new FieldError("request", "INVALID_FORMAT", e.getMessage());
        
        return ResponseEntity.badRequest().body(
            ResponseUtils.validationError(List.of(fieldError), "Invalid request parameters")
        );
    }
    
    /**
     * Handles data quality specific business rule violations.
     */
    @ExceptionHandler(QualityValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleQualityValidationException(QualityValidationException e) {
        logger.warn("Quality validation error: {}", e.getMessage());
        
        return ResponseEntity.badRequest().body(
            ResponseUtils.businessRuleError(e.getMessage(), "data-quality.validation.failed")
        );
    }
    
    /**
     * Handles data inconsistency exceptions.
     */
    @ExceptionHandler(DataInconsistencyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataInconsistencyException(DataInconsistencyException e) {
        logger.error("Data inconsistency detected: {}", e.getMessage());
        
        return ResponseEntity.badRequest().body(
            ResponseUtils.businessRuleError(e.getMessage(), "data-quality.inconsistency.detected")
        );
    }
    
    /**
     * Handles S3 storage exceptions.
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorageException(StorageException e) {
        logger.error("Storage operation failed: {}", e.getMessage(), e);
        
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError("Storage operation failed: " + e.getMessage())
        );
    }
    
    /**
     * Handles quality scoring exceptions.
     */
    @ExceptionHandler(QualityScoringException.class)
    public ResponseEntity<ApiResponse<Void>> handleQualityScoringException(QualityScoringException e) {
        logger.error("Quality scoring failed: {}", e.getMessage(), e);
        
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError("Quality scoring failed: " + e.getMessage())
        );
    }
    
    /**
     * Handles general runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        logger.error("Unexpected runtime error in data quality module: {}", e.getMessage(), e);
        
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError("An unexpected error occurred: " + e.getMessage())
        );
    }
    
    /**
     * Handles general exceptions as fallback.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception e) {
        logger.error("Unexpected error in data quality module: {}", e.getMessage(), e);
        
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError("An unexpected system error occurred")
        );
    }
    
    // Custom exception classes for the data quality module
    
    /**
     * Exception thrown when quality validation fails.
     */
    public static class QualityValidationException extends RuntimeException {
        public QualityValidationException(String message) {
            super(message);
        }
        
        public QualityValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when data inconsistency is detected.
     */
    public static class DataInconsistencyException extends RuntimeException {
        public DataInconsistencyException(String message) {
            super(message);
        }
        
        public DataInconsistencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when storage operations fail.
     */
    public static class StorageException extends RuntimeException {
        public StorageException(String message) {
            super(message);
        }
        
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when quality scoring fails.
     */
    public static class QualityScoringException extends RuntimeException {
        public QualityScoringException(String message) {
            super(message);
        }
        
        public QualityScoringException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}