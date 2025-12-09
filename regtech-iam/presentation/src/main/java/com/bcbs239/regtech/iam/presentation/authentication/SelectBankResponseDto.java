package com.bcbs239.regtech.iam.presentation.authentication;

import com.bcbs239.regtech.iam.application.authentication.SelectBankResponse;
import com.bcbs239.regtech.iam.application.authentication.TenantContextDto;

import java.time.Instant;

/**
 * Presentation DTO for select bank response
 */
public record SelectBankResponseDto(
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    TenantContextDto tenantContext
) {
    /**
     * Converts from application layer SelectBankResponse to presentation DTO
     */
    public static SelectBankResponseDto from(SelectBankResponse response) {
        return new SelectBankResponseDto(
            response.accessToken(),
            response.refreshToken(),
            response.accessTokenExpiresAt(),
            response.refreshTokenExpiresAt(),
            response.tenantContext()
        );
    }
}
