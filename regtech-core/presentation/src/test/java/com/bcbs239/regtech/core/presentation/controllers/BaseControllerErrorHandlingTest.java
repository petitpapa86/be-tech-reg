package com.bcbs239.regtech.core.presentation.controllers;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.FieldError;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BaseController error handling
 * 
 * Verifies that all authentication error codes are properly recognized and handled
 * Requirements: 8.1, 8.2, 8.3
 */
@DisplayName("BaseController Error Handling Tests")
class BaseControllerErrorHandlingTest {

    private TestController controller;

    @BeforeEach
    void setUp() {
        controller = new TestController();
    }

    @Test
    @DisplayName("Should handle INVALID_CREDENTIALS as authentication error")
    void testInvalidCredentialsError() {
        ErrorDetail error = ErrorDetail.of(
            "INVALID_CREDENTIALS",
            ErrorType.AUTHENTICATION_ERROR,
            "Invalid email or password",
            "login.invalid_credentials"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.AUTHENTICATION_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid email or password");
    }

    @Test
    @DisplayName("Should handle ACCOUNT_DISABLED as authentication error")
    void testAccountDisabledError() {
        ErrorDetail error = ErrorDetail.of(
            "ACCOUNT_DISABLED",
            ErrorType.AUTHENTICATION_ERROR,
            "Account is disabled",
            "login.account_disabled"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("Should handle JWT_EXPIRED as authentication error")
    void testJwtExpiredError() {
        ErrorDetail error = ErrorDetail.of(
            "JWT_EXPIRED",
            ErrorType.AUTHENTICATION_ERROR,
            "JWT token has expired",
            "jwt.expired"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("Should handle JWT_INVALID_SIGNATURE as authentication error")
    void testJwtInvalidSignatureError() {
        ErrorDetail error = ErrorDetail.of(
            "JWT_INVALID_SIGNATURE",
            ErrorType.AUTHENTICATION_ERROR,
            "JWT token has invalid signature",
            "jwt.invalid_signature"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("Should handle INVALID_REFRESH_TOKEN as authentication error")
    void testInvalidRefreshTokenError() {
        ErrorDetail error = ErrorDetail.of(
            "INVALID_REFRESH_TOKEN",
            ErrorType.AUTHENTICATION_ERROR,
            "Invalid or expired refresh token",
            "refresh_token.invalid"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("Should handle BANK_ACCESS_DENIED as authentication error")
    void testBankAccessDeniedError() {
        ErrorDetail error = ErrorDetail.of(
            "BANK_ACCESS_DENIED",
            ErrorType.AUTHENTICATION_ERROR,
            "User does not have access to the selected bank",
            "select_bank.access_denied"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("Should handle TOKEN_ALREADY_REVOKED as business rule error")
    void testTokenAlreadyRevokedError() {
        ErrorDetail error = ErrorDetail.of(
            "TOKEN_ALREADY_REVOKED",
            ErrorType.BUSINESS_RULE_ERROR,
            "Refresh token is already revoked",
            "refresh_token.already_revoked"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.BUSINESS_RULE_ERROR);
    }

    @Test
    @DisplayName("Should handle USER_NOT_FOUND as not found error")
    void testUserNotFoundError() {
        ErrorDetail error = ErrorDetail.of(
            "USER_NOT_FOUND",
            ErrorType.NOT_FOUND_ERROR,
            "User not found",
            "user.not_found"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.NOT_FOUND_ERROR);
    }

    @Test
    @DisplayName("Should handle BANK_NOT_FOUND as not found error")
    void testBankNotFoundError() {
        ErrorDetail error = ErrorDetail.of(
            "BANK_NOT_FOUND",
            ErrorType.NOT_FOUND_ERROR,
            "Bank not found",
            "bank.not_found"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.NOT_FOUND_ERROR);
    }

    @Test
    @DisplayName("Should handle validation errors with field details")
    void testValidationErrors() {
        List<FieldError> fieldErrors = List.of(
            new FieldError("email", "Email is required", "login.email.required"),
            new FieldError("password", "Password is required", "login.password.required")
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleValidationError(
            fieldErrors,
            "Validation failed"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrors()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle MISSING_TOKEN as authentication error")
    void testMissingTokenError() {
        ErrorDetail error = ErrorDetail.of(
            "MISSING_TOKEN",
            ErrorType.AUTHENTICATION_ERROR,
            "Authentication token is required",
            "auth.missing_token"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.AUTHENTICATION_ERROR);
    }

    @Test
    @DisplayName("Should handle AUTHENTICATION_ERROR as authentication error")
    void testGenericAuthenticationError() {
        ErrorDetail error = ErrorDetail.of(
            "AUTHENTICATION_ERROR",
            ErrorType.AUTHENTICATION_ERROR,
            "Authentication failed",
            "auth.failed"
        );

        ResponseEntity<? extends ApiResponse<?>> response = controller.handleError(error);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getType()).isEqualTo(ErrorType.AUTHENTICATION_ERROR);
    }

    /**
     * Test implementation of BaseController for testing purposes
     */
    private static class TestController extends BaseController {
        // Expose protected methods for testing
        @Override
        public ResponseEntity<? extends ApiResponse<?>> handleError(ErrorDetail error) {
            return super.handleError(error);
        }

        @Override
        public ResponseEntity<? extends ApiResponse<?>> handleValidationError(
                List<FieldError> fieldErrors,
                String message) {
            return super.handleValidationError(fieldErrors, message);
        }
    }
}
