package com.bcbs239.regtech.iam.presentation.authentication;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.presentation.apiresponses.ApiResponse;
import com.bcbs239.regtech.iam.application.authentication.*;
import com.bcbs239.regtech.iam.domain.authentication.IRefreshTokenRepository;
import com.bcbs239.regtech.iam.domain.authentication.PasswordHasher;
import com.bcbs239.regtech.iam.domain.users.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authentication error handling
 * 
 * Verifies that all error codes are properly handled and return consistent JSON responses
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
@WebMvcTest(AuthenticationController.class)
@DisplayName("Authentication Error Handling Tests")
class AuthenticationErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginCommandHandler loginHandler;

    @MockBean
    private RefreshTokenCommandHandler refreshTokenHandler;

    @MockBean
    private SelectBankCommandHandler selectBankHandler;

    @MockBean
    private LogoutCommandHandler logoutHandler;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private IRefreshTokenRepository refreshTokenRepository;

    @MockBean
    private PasswordHasher passwordHasher;

    @Test
    @DisplayName("Should return INVALID_CREDENTIALS error with generic message")
    void testInvalidCredentialsError() throws Exception {
        // This test verifies Requirement 8.1: Generic error messages for authentication failures
        
        String loginRequest = """
            {
                "email": "nonexistent@example.com",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.type").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Should return ACCOUNT_DISABLED error")
    void testAccountDisabledError() throws Exception {
        // This test verifies Requirement 8.1: Appropriate error for disabled accounts
        
        String loginRequest = """
            {
                "email": "disabled@example.com",
                "password": "password123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.type").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    @Test
    @DisplayName("Should return INVALID_REFRESH_TOKEN error with generic message")
    void testInvalidRefreshTokenError() throws Exception {
        // This test verifies Requirement 8.1: Generic error for refresh token failures
        
        String refreshRequest = """
            {
                "refreshToken": "invalid-token"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.type").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    @DisplayName("Should return BANK_ACCESS_DENIED error")
    void testBankAccessDeniedError() throws Exception {
        // This test verifies proper authorization error handling
        
        String selectBankRequest = """
            {
                "userId": "user-123",
                "bankId": "bank-456",
                "refreshToken": "valid-token"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/select-bank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(selectBankRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.type").value("AUTHENTICATION_ERROR"))
                .andExpect(jsonPath("$.message").value("User does not have access to the selected bank"));
    }

    @Test
    @DisplayName("Should return validation errors with field details")
    void testValidationErrors() throws Exception {
        // This test verifies Requirement 8.3: Detailed field-level errors for validation
        
        String invalidLoginRequest = """
            {
                "email": "",
                "password": ""
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidLoginRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.type").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("Should return consistent JSON error response format")
    void testConsistentErrorResponseFormat() throws Exception {
        // This test verifies Requirement 8.3: Consistent JSON error responses
        
        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.type").exists());
    }

    @Test
    @DisplayName("Should not expose sensitive information in error messages")
    void testNoSensitiveInformationInErrors() throws Exception {
        // This test verifies Requirement 8.1, 8.4: No user enumeration or sensitive data exposure
        
        String loginRequest = """
            {
                "email": "test@example.com",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                // Verify message doesn't reveal whether email exists
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("user not found")
                )))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString("email not found")
                )));
    }
}
