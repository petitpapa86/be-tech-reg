package com.bcbs239.regtech.iam.domain.authentication;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.time.Instant;

/**
 * IRefreshTokenRepository interface - defines refresh token persistence operations
 */
public interface IRefreshTokenRepository {
    /**
     * Saves a refresh token
     */
    Result<RefreshTokenId> save(RefreshToken refreshToken);

    /**
     * Finds a refresh token by ID
     */
    Maybe<RefreshToken> findById(RefreshTokenId id);

    /**
     * Finds a refresh token by token hash
     */
    Maybe<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes all refresh tokens for a user
     */
    Result<Void> revokeAllForUser(UserId userId);

    /**
     * Deletes expired tokens older than the specified timestamp
     */
    Result<Void> deleteExpiredTokens(Instant olderThan);
}
