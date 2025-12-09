package com.bcbs239.regtech.iam.domain.authentication;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for RefreshToken aggregate
 * Defines the contract for refresh token persistence operations
 */
public interface IRefreshTokenRepository {
    
    /**
     * Saves a refresh token to the database
     * @param refreshToken the refresh token to save
     * @return Result containing the saved token's ID or an error
     */
    Result<RefreshTokenId> save(RefreshToken refreshToken);
    
    /**
     * Finds a refresh token by its ID
     * @param id the refresh token ID
     * @return Maybe containing the refresh token if found, or empty
     */
    Maybe<RefreshToken> findById(RefreshTokenId id);
    
    /**
     * Finds a refresh token by its hash
     * @param tokenHash the BCrypt hash of the token value
     * @return Maybe containing the refresh token if found, or empty
     */
    Maybe<RefreshToken> findByTokenHash(String tokenHash);
    
    /**
     * Finds all valid (non-revoked, non-expired) refresh tokens for a user
     * @param userId the user ID
     * @return List of valid refresh tokens
     */
    List<RefreshToken> findValidTokensByUserId(UserId userId);
    
    /**
     * Revokes all refresh tokens for a specific user
     * @param userId the user ID
     * @return Result indicating success or failure
     */
    Result<Void> revokeAllForUser(UserId userId);
    
    /**
     * Deletes expired tokens older than the specified date
     * @param olderThan the cutoff date for deletion
     * @return Result indicating success or failure
     */
    Result<Void> deleteExpiredTokens(Instant olderThan);
}
