package com.bcbs239.regtech.billing.infrastructure.validation;

import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception handler for billing validation errors.
 * Provides consistent error responses for validation failures in the billing module.
 */
@RestControllerAdvice(basePackages = "com.bcbs239.regtech.billing.api")
public class BillingValidationExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations on request bodies
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = new ArrayList<>();
        
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(ErrorDetail.of(
                "VALIDATION_ERROR",
                String.format("Field '%s': %s", error.getField(), error.getDefaultMessage()),
                "validation.field.error"
            ));
        }
        
        String message = errors.size() == 1 
            ? errors.get(0).getMessage()
            : String.format("Validation failed for %d fields", errors.size());
            
        return ResponseEntity.badRequest().body(
            ApiResponse.validationError(message, errors)
        );
    }

    /**
     * Handle validation errors from method-level validation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDetail> errors = ex.getConstraintViolations().stream()
            .map(this::mapConstraintViolation)
            .collect(Collectors.toList());
        
        String message = errors.size() == 1 
            ? errors.get(0).getMessage()
            : String.format("Validation failed for %d constraints", errors.size());
            
        return ResponseEntity.badRequest().body(
            ApiResponse.validationError(message, errors)
        );
    }

    /**
     * Handle illegal argument exceptions that may come from validation utilities
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorDetail error = ErrorDetail.of(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            "validation.invalid.argument"
        );
        
        return ResponseEntity.badRequest().body(
            ApiResponse.validationError(ex.getMessage(), List.of(error))
        );
    }

    /**
     * Map constraint violation to ErrorDetail
     */
    private ErrorDetail mapConstraintViolation(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String message = violation.getMessage();
        
        return ErrorDetail.of(
            "CONSTRAINT_VIOLATION",
            String.format("Property '%s': %s", propertyPath, message),
            "validation.constraint.violation"
        );
    }
}