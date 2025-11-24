package com.bcbs239.regtech.iam.domain.authentication;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.JwtToken;
import com.bcbs239.regtech.iam.domain.users.TenantContext;
import com.bcbs239.regtech.iam.domain.users.User;

import java.time.Duration;

/**
 * TokenPair Value Object - represents an access token and refresh token pair
 */
public record TokenPair(
    JwtToken accessToken,
    RefreshToken refreshToken
) {
    /**
     * Creates a new token pair for a user
     */
    public static Result<TokenPair> create(
        User user,
        String jwtSecret,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration,
        PasswordHasher passwordHasher
    ) {
        // Generate access token
        Result<JwtToken> accessTokenResult = JwtToken.generate(
            user,
            jwtSecret,
            accessTokenExpiration
        );

        if (accessTokenResult.isFailure()) {
            return Result.failure(accessTokenResult.getError().get());
        }

        // Generate refresh token value
        String tokenValue = RefreshToken.generateSecureToken();
        String tokenHash = passwordHasher.hash(tokenValue);

        // Generate refresh token
        Result<RefreshToken> refreshTokenResult = RefreshToken.create(
            user.getId(),
            tokenValue,
            tokenHash,
            refreshTokenExpiration
        );

        if (refreshTokenResult.isFailure()) {
            return Result.failure(refreshTokenResult.getError().get());
        }

        return Result.success(new TokenPair(
            accessTokenResult.getValue().get(),
            refreshTokenResult.getValue().get()
        ));
    }
    
    /**
     * Creates a new token pair for a user with specific tenant context
     * Used for bank selection flow
     */
    public static Result<TokenPair> createWithTenantContext(
        User user,
        TenantContext tenantContext,
        String jwtSecret,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration,
        PasswordHasher passwordHasher
    ) {
        // Generate access token with tenant context
        Result<JwtToken> accessTokenResult = JwtToken.generateWithTenantContext(
            user,
            tenantContext,
            jwtSecret,
            accessTokenExpiration
        );

        if (accessTokenResult.isFailure()) {
            return Result.failure(accessTokenResult.getError().get());
        }

        // Generate refresh token value
        String tokenValue = RefreshToken.generateSecureToken();
        String tokenHash = passwordHasher.hash(tokenValue);

        // Generate refresh token
        Result<RefreshToken> refreshTokenResult = RefreshToken.create(
            user.getId(),
            tokenValue,
            tokenHash,
            refreshTokenExpiration
        );

        if (refreshTokenResult.isFailure()) {
            return Result.failure(refreshTokenResult.getError().get());
        }

        return Result.success(new TokenPair(
            accessTokenResult.getValue().get(),
            refreshTokenResult.getValue().get()
        ));
    }
}
