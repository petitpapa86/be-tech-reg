package com.bcbs239.regtech.iam.presentation.authentication;

/**
 * Request DTO for token refresh
 */
public record RefreshTokenRequest(
    String refreshToken
) {}
