package com.bcbs239.regtech.iam.domain.authentication;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.shared.*;
import com.bcbs239.regtech.iam.domain.authentication.events.RefreshTokenCreatedEvent;
import com.bcbs239.regtech.iam.domain.authentication.events.RefreshTokenRevokedEvent;
import com.bcbs239.regtech.iam.domain.users.UserId;
import lombok.Getter;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * RefreshToken Aggregate - represents a refresh token with lifecycle management
 */
@Getter
public class RefreshToken extends Entity {
    private final RefreshTokenId id;
    private final UserId userId;
    private final String tokenHash;  // BCrypt hash of token value
    private final String tokenValue; // Plain token value (only available at creation)
    private final Instant expiresAt;
    private final Instant createdAt;
    private boolean revoked;
    private Maybe<Instant> revokedAt;

    private RefreshToken(
            RefreshTokenId id,
            UserId userId,
            String tokenHash,
            String tokenValue,
            Instant expiresAt,
            Instant createdAt,
            boolean revoked,
            Maybe<Instant> revokedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.tokenValue = tokenValue;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
    }

    /**
     * Factory method to create a new refresh token
     */
    public static Result<RefreshToken> create(
            UserId userId,
            String tokenValue,
            String tokenHash,
            Duration expiration
    ) {
        // Validation
        if (userId == null) {
            return Result.failure(ErrorDetail.of(
                    "USER_ID_REQUIRED",
                    ErrorType.VALIDATION_ERROR,
                    "User ID is required",
                    "refresh_token.user_id.required"
            ));
        }

        if (tokenValue == null || tokenValue.isBlank()) {
            return Result.failure(ErrorDetail.of(
                    "TOKEN_VALUE_REQUIRED",
                    ErrorType.VALIDATION_ERROR,
                    "Token value is required",
                    "refresh_token.value.required"
            ));
        }

        if (tokenHash == null || tokenHash.isBlank()) {
            return Result.failure(ErrorDetail.of(
                    "TOKEN_HASH_REQUIRED",
                    ErrorType.VALIDATION_ERROR,
                    "Token hash is required",
                    "refresh_token.hash.required"
            ));
        }

        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            return Result.failure(ErrorDetail.of(
                    "INVALID_EXPIRATION",
                    ErrorType.VALIDATION_ERROR,
                    "Expiration must be positive",
                    "refresh_token.expiration.invalid"
            ));
        }

        RefreshTokenId id = RefreshTokenId.generate();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);

        RefreshToken refreshToken = new RefreshToken(
                id,
                userId,
                tokenHash,
                tokenValue,
                expiresAt,
                now,
                false,
                Maybe.none()
        );

        // Raise domain event
        refreshToken.addDomainEvent(new RefreshTokenCreatedEvent(
                id,
                userId,
                expiresAt
        ));

        return Result.success(refreshToken);
    }

    /**
     * Factory method for persistence layer reconstruction
     */
    public static RefreshToken createFromPersistence(
            RefreshTokenId id,
            UserId userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            boolean revoked,
            Maybe<Instant> revokedAt
    ) {
        return new RefreshToken(
                id,
                userId,
                tokenHash,
                null, // Token value not available from persistence
                expiresAt,
                createdAt,
                revoked,
                revokedAt
        );
    }

    /**
     * Generates a cryptographically secure random token value
     */
    public static String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Revokes the refresh token
     */
    public Result<Void> revoke() {
        if (this.revoked) {
            return Result.failure(ErrorDetail.of(
                    "TOKEN_ALREADY_REVOKED",
                    ErrorType.BUSINESS_RULE_ERROR,
                    "Refresh token is already revoked",
                    "refresh_token.already_revoked"
            ));
        }

        this.revoked = true;
        this.revokedAt = Maybe.some(Instant.now());

        // Raise domain event
        this.addDomainEvent(new RefreshTokenRevokedEvent(
                this.id,
                this.userId,
                this.revokedAt.getValue()
        ));

        return Result.success(null);
    }

    /**
     * Checks if the token is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * Checks if the token is valid (not revoked and not expired)
     */
    public boolean isValid() {
        return !this.revoked && !this.isExpired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + userId +
                ", expiresAt=" + expiresAt +
                ", revoked=" + revoked +
                '}';
    }
}
