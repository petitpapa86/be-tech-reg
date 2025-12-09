package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.iam.domain.authentication.RefreshToken;
import com.bcbs239.regtech.iam.domain.users.JwtToken;
import com.bcbs239.regtech.iam.domain.users.TenantContext;

import java.time.Instant;

/**
 * SelectBankResponse - Response after successful bank selection
 * 
 * Requirements: 3.5
 */
public record SelectBankResponse(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    TenantContextDto tenantContext
) {
    /**
     * Creates a SelectBankResponse from domain objects
     */
    public static SelectBankResponse from(
        JwtToken accessToken,
        RefreshToken refreshToken,
        TenantContext tenantContext
    ) {
        return new SelectBankResponse(
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            TenantContextDto.from(tenantContext)
        );
    }
}
