package com.bcbs239.regtech.iam.application.authentication;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.iam.domain.authentication.RefreshToken;
import com.bcbs239.regtech.iam.domain.users.JwtToken;
import com.bcbs239.regtech.iam.domain.users.TenantContext;
import com.bcbs239.regtech.iam.domain.users.User;

import java.time.Instant;
import java.util.List;

/**
 * LoginResponse - Response DTO for login operation
 * 
 * Contains user information, tokens, and bank selection requirements
 * Requirements: 1.1, 1.2, 1.3, 10.1, 10.2
 */
public record LoginResponse(
    String userId,
    String email,
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiresAt,
    Instant refreshTokenExpiresAt,
    boolean requiresBankSelection,
    List<BankAssignmentDto> availableBanks,
    Maybe<TenantContext> tenantContext,
    String nextStep
) {
    /**
     * Creates a response for a user with a single bank assignment
     */
    public static LoginResponse withSingleBank(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken,
        TenantContext tenantContext
    ) {
        return new LoginResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            false,
            List.of(),
            Maybe.some(tenantContext),
            "DASHBOARD"
        );
    }
    
    /**
     * Creates a response for a user with multiple bank assignments
     */
    public static LoginResponse withMultipleBanks(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken,
        List<BankAssignmentDto> availableBanks
    ) {
        return new LoginResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            true,
            availableBanks,
            Maybe.none(),
            "SELECT_BANK"
        );
    }
    
    /**
     * Creates a response for a user with no bank assignments
     */
    public static LoginResponse withNoBanks(
        User user,
        JwtToken accessToken,
        RefreshToken refreshToken
    ) {
        return new LoginResponse(
            user.getId().getValue(),
            user.getEmail().getValue(),
            accessToken.value(),
            refreshToken.getTokenValue(),
            accessToken.expiresAt(),
            refreshToken.getExpiresAt(),
            false,
            List.of(),
            Maybe.none(),
            "CONFIGURE_BANK"
        );
    }
}
