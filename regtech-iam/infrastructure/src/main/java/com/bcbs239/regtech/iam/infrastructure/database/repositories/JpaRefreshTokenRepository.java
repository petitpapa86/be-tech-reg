package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.authentication.IRefreshTokenRepository;
import com.bcbs239.regtech.iam.domain.authentication.RefreshToken;
import com.bcbs239.regtech.iam.domain.authentication.RefreshTokenId;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.infrastructure.database.entities.RefreshTokenEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of IRefreshTokenRepository
 * Handles persistence operations for RefreshToken aggregate
 */
@Repository
public class JpaRefreshTokenRepository implements IRefreshTokenRepository {
    
    private static final Logger log = LoggerFactory.getLogger(JpaRefreshTokenRepository.class);
    
    private final SpringDataRefreshTokenRepository jpaRepository;
    private final RefreshTokenMapper mapper;
    
    public JpaRefreshTokenRepository(
        SpringDataRefreshTokenRepository jpaRepository,
        RefreshTokenMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public Result<RefreshTokenId> save(RefreshToken refreshToken) {
        try {
            RefreshTokenEntity entity = mapper.toEntity(refreshToken);
            RefreshTokenEntity saved = jpaRepository.save(entity);
            
            log.info("REFRESH_TOKEN_SAVED - tokenId: {}, userId: {}, hashPreview: {}..., hashLength: {}", 
                saved.getId(),
                saved.getUserId(),
                saved.getTokenHash().substring(0, Math.min(10, saved.getTokenHash().length())),
                saved.getTokenHash().length());
            
            return Result.success(new RefreshTokenId(saved.getId()));
        } catch (Exception e) {
            log.error("REFRESH_TOKEN_SAVE_FAILED - tokenId: {}, error: {}", 
                refreshToken.getId().value(),
                e.getMessage(),
                e);
            return Result.failure(ErrorDetail.of(
                "SAVE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to save refresh token",
                "refresh_token.save.failed"
            ));
        }
    }
    
    @Override
    public Maybe<RefreshToken> findById(RefreshTokenId id) {
        try {
            return jpaRepository.findById(id.value())
                .map(mapper::toDomain)
                .map(Maybe::some)
                .orElse(Maybe.none());
        } catch (Exception e) {
            log.error("REFRESH_TOKEN_FIND_BY_ID_FAILED - tokenId: {}, error: {}", 
                id.value(),
                e.getMessage(),
                e);
            return Maybe.none();
        }
    }
    
    @Override
    public Maybe<RefreshToken> findByTokenHash(String tokenHash) {
        try {
            return jpaRepository.findByTokenHash(tokenHash)
                .map(mapper::toDomain)
                .map(Maybe::some)
                .orElse(Maybe.none());
        } catch (Exception e) {
            log.error("REFRESH_TOKEN_FIND_BY_HASH_FAILED - tokenHash: {}..., error: {}", 
                tokenHash.substring(0, Math.min(10, tokenHash.length())),
                e.getMessage(),
                e);
            return Maybe.none();
        }
    }
    
    @Override
    public List<RefreshToken> findValidTokensByUserId(UserId userId) {
        try {
            UUID userUuid = UUID.fromString(userId.getValue());
            Instant now = Instant.now();
            
            List<RefreshTokenEntity> entities = jpaRepository.findByUserId(userUuid);
            
            return entities.stream()
                .filter(entity -> !entity.isRevoked() && entity.getExpiresAt().isAfter(now))
                .map(mapper::toDomain)
                .toList();
        } catch (Exception e) {
            log.error("REFRESH_TOKEN_FIND_VALID_BY_USER_FAILED - userId: {}, error: {}", 
                userId.getValue(),
                e.getMessage(),
                e);
            return List.of();
        }
    }
    
    @Override
    @Transactional
    public Result<Void> revokeAllForUser(UserId userId) {
        try {
            UUID userUuid = UUID.fromString(userId.getValue());
            List<RefreshTokenEntity> tokens = jpaRepository.findByUserId(userUuid);
            
            Instant now = Instant.now();
            tokens.forEach(token -> {
                token.setRevoked(true);
                token.setRevokedAt(now);
            });
            
            jpaRepository.saveAll(tokens);
            
            log.info("REVOKE_ALL_TOKENS_SUCCESS - userId: {}, tokensRevoked: {}", 
                userId.getValue(),
                tokens.size());
            
            return Result.success(null);
        } catch (Exception e) {
            log.error("REVOKE_ALL_TOKENS_FAILED - userId: {}, error: {}", 
                userId.getValue(),
                e.getMessage(),
                e);
            return Result.failure(ErrorDetail.of(
                "REVOKE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to revoke tokens",
                "refresh_token.revoke.failed"
            ));
        }
    }
    
    @Override
    @Transactional
    public Result<Void> deleteExpiredTokens(Instant olderThan) {
        try {
            jpaRepository.deleteByExpiresAtBefore(olderThan);
            
            log.info("DELETE_EXPIRED_TOKENS_SUCCESS - olderThan: {}", 
                olderThan);
            
            return Result.success(null);
        } catch (Exception e) {
            log.error("DELETE_EXPIRED_TOKENS_FAILED - olderThan: {}, error: {}", 
                olderThan,
                e.getMessage(),
                e);
            return Result.failure(ErrorDetail.of(
                "DELETE_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to delete expired tokens",
                "refresh_token.delete.failed"
            ));
        }
    }
}
