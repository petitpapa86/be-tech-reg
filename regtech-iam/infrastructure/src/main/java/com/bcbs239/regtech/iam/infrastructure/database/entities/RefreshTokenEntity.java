package com.bcbs239.regtech.iam.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import java.util.UUID;

/**
 * JPA Entity for refresh_tokens table
 * Stores refresh tokens for OAuth 2.0 token refresh flow
 */
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens", schema = "iam", indexes = {
    @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash"),
    @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
})
public class RefreshTokenEntity {
    
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "revoked", nullable = false)
    private boolean revoked;
    
    @Column(name = "revoked_at")
    private Instant revokedAt;
    
    /**
     * Default constructor for JPA
     */
    public RefreshTokenEntity() {}
    
    /**
     * Constructor with all fields
     */
    public RefreshTokenEntity(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt,
        boolean revoked,
        Instant revokedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
    }
}
