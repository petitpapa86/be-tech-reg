package com.bcbs239.regtech.iam.presentation.authentication;

/**
 * Request DTO for user logout
 */
public record LogoutRequest(
    String refreshToken  // Optional - can revoke specific token or all tokens
) {}
