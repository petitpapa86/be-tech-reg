package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.iam.domain.authentication.RefreshToken;
import com.bcbs239.regtech.iam.domain.users.JwtToken;

import java.time.Instant;

/**
 * Response DTO for token refresh operation
 * Requirements: 2.1, 2.5
 */
public record RefreshTokenResponse(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    Maybe<TenantContextDto> tenantContext
) {
    /**
     * Factory method to create response from domain objects
     */
    public static RefreshTokenResponse from(
        JwtToken accessToken,
        RefreshToken refreshToken,
        Maybe<TenantContextDto> tenantContext
    ) {
        return new RefreshTokenResponse(
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            tenantContext
        );
    }
}
