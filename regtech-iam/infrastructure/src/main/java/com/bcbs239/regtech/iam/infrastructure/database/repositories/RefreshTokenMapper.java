package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.iam.domain.authentication.RefreshToken;
import com.bcbs239.regtech.iam.domain.authentication.RefreshTokenId;
import com.bcbs239.regtech.iam.domain.users.UserId;
import com.bcbs239.regtech.iam.infrastructure.database.entities.RefreshTokenEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper between RefreshToken domain model and RefreshTokenEntity
 * Handles conversion between domain and persistence layers
 */
@Component
public class RefreshTokenMapper {
    
    /**
     * Converts a RefreshToken domain object to a RefreshTokenEntity
     * @param refreshToken the domain object
     * @return the entity
     */
    public RefreshTokenEntity toEntity(RefreshToken refreshToken) {
        return new RefreshTokenEntity(
            refreshToken.getId().value(),
            refreshToken.getUserId().getUUID(),
            refreshToken.getTokenHash(),
            refreshToken.getExpiresAt(),
            refreshToken.getCreatedAt(),
            refreshToken.isRevoked(),
            refreshToken.getRevokedAt().isPresent() ? refreshToken.getRevokedAt().getValue() : null
        );
    }
    
    /**
     * Converts a RefreshTokenEntity to a RefreshToken domain object
     * @param entity the entity
     * @return the domain object
     */
    public RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.createFromPersistence(
            new RefreshTokenId(entity.getId()),
            new UserId(entity.getUserId()),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.getCreatedAt(),
            entity.isRevoked(),
            entity.getRevokedAt() != null ? Maybe.some(entity.getRevokedAt()) : Maybe.none()
        );
    }
}
