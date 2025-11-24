package com.bcbs239.regtech.iam.presentation.authentication;

import com.bcbs239.regtech.iam.application.authentication.RefreshTokenResponse;
import com.bcbs239.regtech.iam.application.authentication.TenantContextDto;

import java.time.Instant;

/**
 * Presentation DTO for refresh token response
 */
public record RefreshTokenResponseDto(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    TenantContextDto tenantContext
) {
    /**
     * Converts from application layer RefreshTokenResponse to presentation DTO
     */
    public static RefreshTokenResponseDto from(RefreshTokenResponse response) {
        TenantContextDto tenantContext = response.tenantContext().isPresent()
            ? response.tenantContext().getValue()
            : null;
        
        return new RefreshTokenResponseDto(
            response.accessToken(),
            response.refreshToken(),
            response.accessTokenExpiresAt(),
            response.refreshTokenExpiresAt(),
            tenantContext
        );
    }
}
