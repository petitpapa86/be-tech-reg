package com.bcbs239.regtech.iam.application.authentication;

/**
 * Result of OAuth2 authentication
 */
public record OAuth2AuthenticationResult(
    String provider,
    String providerUserId,
    String email,
    String name,
    String accessToken,
    String refreshToken
) {}

