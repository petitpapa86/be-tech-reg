package com.bcbs239.regtech.core.domain.shared;

/**
 * Enumeration of error types for API responses.
 * Helps frontend distinguish how to handle different types of errors.
 */
public enum ErrorType {
    /**
     * Validation errors (e.g., form field errors, input validation)
     * Frontend should show inline field errors
     */
    VALIDATION_ERROR,

    /**
     * Business rule violations (e.g., duplicate email, insufficient funds)
     * Frontend should show toast/snackbar notifications
     */
    BUSINESS_RULE_ERROR,

    /**
     * System errors (e.g., database connection, external service failures)
     * Frontend should show global error handling (modal, fallback page)
     */
    SYSTEM_ERROR,

    /**
     * Authentication/authorization errors
     * Frontend should redirect to login or show access denied
     */
    AUTHENTICATION_ERROR,

    /**
     * Not found errors
     * Frontend should show 404 page or not found message
     */
    NOT_FOUND_ERROR
}

