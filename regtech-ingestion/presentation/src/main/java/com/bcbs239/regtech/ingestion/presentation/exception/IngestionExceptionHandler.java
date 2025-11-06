package com.bcbs239.regtech.ingestion.presentation.exception;


import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception handler for ingestion module errors.
 * Provides consistent error responses using existing ErrorDetail and FieldError classes.
 * Returns structured error responses using existing ApiResponse format.
 */
@RestControllerAdvice(basePackages = "com.bcbs239.regtech.modules.ingestion.presentation")
public class IngestionExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(IngestionExceptionHandler.class);

    /**
     * Handle validation errors from @Valid annotations on request bodies.
     * Uses existing ErrorDetail and FieldError classes for consistent formatting.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        logger.debug("Handling method argument validation error: {}", ex.getMessage());
        
        List<FieldError> errors = new ArrayList<>();

        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldError(
                error.getField(),
                error.getDefaultMessage(), "ingestion.validation.invalid."+error.getField())
            );
        }

        String message = errors.size() == 1
            ? errors.getFirst().message()
            : String.format("Validation failed for %d fields", errors.size());

        return ResponseEntity.badRequest().body(
            ResponseUtils.validationError(errors, message)
        );
    }

    /**
     * Handle validation errors from method-level validation.
     * Uses existing infrastructure for consistent error formatting.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        logger.debug("Handling constraint violation error: {}", ex.getMessage());
        
        List<FieldError> errors = ex.getConstraintViolations().stream()
            .map(this::mapConstraintViolationToFieldError)
            .collect(Collectors.toList());

        String message = errors.size() == 1
            ? errors.getFirst().message()
            : String.format("Validation failed for %d constraints", errors.size());

        return ResponseEntity.badRequest().body(
            ResponseUtils.validationError(errors, message)
        );
    }

    /**
     * Handle illegal argument exceptions from command/query validation.
     * Returns structured error responses using existing ApiResponse format.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.debug("Handling illegal argument error: {}", ex.getMessage());
        
        FieldError error = new FieldError(
            "argument",
            ex.getMessage(),
            "ingestion.validation.invalid.argument"
        );

        return ResponseEntity.badRequest().body(
            ResponseUtils.validationError(List.of(error), ex.getMessage())
        );
    }

    /**
     * Handle file upload size exceeded exceptions.
     * Returns HTTP 413 Payload Too Large with detailed error message and remediation suggestions.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        logger.warn("File upload size exceeded: {}", ex.getMessage());
        
        FieldError error = new FieldError(
            "file",
            "File size exceeds maximum limit of 500MB. Consider splitting the file into smaller chunks.",
            "ingestion.validation.file.too.large"
        );

        return ResponseEntity.status(413).body(
            ResponseUtils.validationError(List.of(error), 
                "File size exceeds maximum allowed limit. Please split your file and try again.")
        );
    }

    /**
     * Handle runtime exceptions that may occur during processing.
     * Uses existing infrastructure for consistent error handling.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        logger.error("Handling runtime exception in ingestion module: {}", ex.getMessage(), ex);
        
        return ResponseEntity.internalServerError().body(
            ResponseUtils.systemError("An unexpected error occurred during file processing: " + ex.getMessage())
        );
    }

    /**
     * Map constraint violation to FieldError using existing infrastructure.
     */
    private FieldError mapConstraintViolationToFieldError(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String message = violation.getMessage();

        return new FieldError(
            propertyPath,
            message,
            "ingestion.validation.constraint.violation"
        );
    }
}

