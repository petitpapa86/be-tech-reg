package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.iam.infrastructure.database.entities.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for RefreshTokenEntity
 * Provides database access methods for refresh token operations
 */
@Repository
public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    
    /**
     * Finds a refresh token by its hash
     * @param tokenHash the BCrypt hash of the token value
     * @return Optional containing the token entity if found
     */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    
    /**
     * Finds all refresh tokens for a specific user
     * @param userId the user ID
     * @return List of refresh token entities
     */
    List<RefreshTokenEntity> findByUserId(UUID userId);
    
    /**
     * Deletes all refresh tokens that expired before the specified date
     * @param expiresAt the cutoff date
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :expiresAt")
    void deleteByExpiresAtBefore(@Param("expiresAt") Instant expiresAt);
}
