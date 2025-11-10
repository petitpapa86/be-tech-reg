package com.bcbs239.regtech.billing.infrastructure.validation;

import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
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
        List<FieldError> errors = new ArrayList<>();

        for (org.springframework.validation.FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldError(
                error.getField(),
                "VALIDATION_ERROR",
                error.getDefaultMessage(),
                "validation.field.error"
            ));
        }

        String message = errors.size() == 1
            ? errors.get(0).getMessage()
            : String.format("Validation failed for %d fields", errors.size());

        return ResponseEntity.badRequest().body(
            ApiResponse.validationError(errors, message)
        );
    }

    /**
     * Handle validation errors from method-level validation
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> errors = ex.getConstraintViolations().stream()
            .map(this::mapConstraintViolationToFieldError)
            .collect(Collectors.toList());

        String message = errors.size() == 1
            ? errors.get(0).getMessage()
            : String.format("Validation failed for %d constraints", errors.size());

        return ResponseEntity.badRequest().body(
            ApiResponse.validationError(errors, message)
        );
    }    /**
     * Handle illegal argument exceptions that may come from validation utilities
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        com.bcbs239.regtech.core.shared.FieldError error = new com.bcbs239.regtech.core.shared.FieldError(
            "argument",
            "INVALID_ARGUMENT",
            ex.getMessage(),
            "validation.invalid.argument"
        );

        return ResponseEntity.badRequest().body(
            ApiResponse.validationError(List.of(error), ex.getMessage())
        );
    }

    /**
     * Map constraint violation to FieldError
     */
    private FieldError mapConstraintViolationToFieldError(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        String message = violation.getMessage();

        return new FieldError(
            propertyPath,
            "CONSTRAINT_VIOLATION",
            message,
            "validation.constraint.violation"
        );
    }
}

