package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.security.authorization.Role;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserRoleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Function;

/**
 * Consolidated JPA repository for User aggregate operations using EntityManager and closures.
 * Follows the established architecture patterns with proper domain/persistence separation.
 */
@Repository
public class JpaUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Closure to find user by email
     * Returns Maybe<User> to handle optional results functionally
     */
    public Function<Email, Maybe<User>> emailLookup() {
        return email -> {
            try {
                UserEntity entity = entityManager.createQuery(
                    "SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
                    .setParameter("email", email.getValue())
                    .getSingleResult();
                return Maybe.some(entity.toDomain());
            } catch (NoResultException e) {
                return Maybe.none();
            } catch (Exception e) {
                return Maybe.none();
            }
        };
    }

    /**
     * Closure to load user by ID
     * Returns Result<User> for functional error handling
     */
    public Function<UserId, Result<User>> userLoader() {
        return userId -> {
            try {
                UserEntity entity = entityManager.find(UserEntity.class, userId.getValue());
                if (entity == null) {
                    return Result.failure(ErrorDetail.of("USER_NOT_FOUND",
                        "User not found with ID: " + userId, "error.user.notFound"));
                }
                return Result.success(entity.toDomain());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("USER_LOAD_FAILED",
                    "Failed to load user: " + e.getMessage(), "error.user.loadFailed"));
            }
        };
    }

    /**
     * Closure to save user
     * Returns Result<UserId> for functional error handling
     */
    public Function<User, Result<UserId>> userSaver() {
        return user -> {
            try {
                UserEntity entity = UserEntity.fromDomain(user);
                entity.setId(null);
                entityManager.persist(entity);
                entityManager.flush();
                return Result.success(user.getId());
            } catch (Exception e) {
                throw new RuntimeException("USER_SAVE_FAILED: Failed to save user: " + e.getMessage(), e);
            }
        };
    }

    /**
     * Closure to find all active roles for a user
     */
    public Function<UserId, List<UserRole>> userRolesFinder() {
        return userId -> {
            try {
                List<UserRoleEntity> entities = entityManager.createQuery(
                    "SELECT ur FROM UserRoleEntity ur WHERE ur.userId = :userId AND ur.active = true", 
                    UserRoleEntity.class)
                    .setParameter("userId", userId.getValue())
                    .getResultList();
                
                return entities.stream()
                    .map(UserRoleEntity::toDomain)
                    .toList();
            } catch (Exception e) {
                return List.of();
            }
        };
    }

    /**
     * Closure to find roles for a user in a specific organization
     */
    public Function<UserOrgQuery, List<UserRole>> userOrgRolesFinder() {
        return query -> {
            try {
                List<UserRoleEntity> entities = entityManager.createQuery(
                    "SELECT ur FROM UserRoleEntity ur WHERE ur.userId = :userId AND ur.organizationId = :organizationId AND ur.active = true", 
                    UserRoleEntity.class)
                    .setParameter("userId", query.userId().getValue())
                    .setParameter("organizationId", query.organizationId())
                    .getResultList();
                
                return entities.stream()
                    .map(UserRoleEntity::toDomain)
                    .toList();
            } catch (Exception e) {
                return List.of();
            }
        };
    }

    /**
     * Closure to save user role
     */
    public Function<UserRole, Result<String>> userRoleSaver() {
        return userRole -> {
            try {
                UserRoleEntity entity = UserRoleEntity.fromDomain(userRole);
                
                if (entity.getId() == null) {
                    entityManager.persist(entity);
                } else {
                    entity = entityManager.merge(entity);
                }
                
                entityManager.flush();
                return Result.success(entity.getId());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("USER_ROLE_SAVE_FAILED",
                    "Failed to save user role: " + e.getMessage(), "error.user.role.saveFailed"));
            }
        };
    }

    /**
     * Closure to check if user has a specific role
     */
    public Function<UserRoleQuery, Boolean> userRoleChecker() {
        return query -> {
            try {
                Long count = entityManager.createQuery(
                    "SELECT COUNT(ur) FROM UserRoleEntity ur WHERE ur.userId = :userId AND ur.role = :role AND ur.active = true", 
                    Long.class)
                    .setParameter("userId", query.userId().getValue())
                    .setParameter("role", query.role().name())
                    .getSingleResult();
                
                return count > 0;
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Traditional method for complex queries that don't fit the closure pattern
     */
    public List<User> findByStatus(UserStatus status) {
        return entityManager.createQuery(
            "SELECT u FROM UserEntity u WHERE u.status = :status", UserEntity.class)
            .setParameter("status", status.name())
            .getResultList()
            .stream()
            .map(UserEntity::toDomain)
            .toList();
    }



    /**
     * Query record for user-organization role lookup
     */
    public record UserOrgQuery(UserId userId, String organizationId) {}

    public Function<User, Result<JwtToken>> tokenGenerator(String jwtSecretKey) {
        return user -> {
            try {
                // Create JWT token with user information
                Result<JwtToken> tokenResult = JwtToken.generate(
                    user,
                    jwtSecretKey,
                    java.time.Duration.ofHours(24)
                );
                return tokenResult;
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("TOKEN_GENERATION_FAILED",
                    "Failed to generate JWT token: " + e.getMessage(),
                    "error.token.generationFailed"));
            }
        };
    }

    /**
     * Query record for user role checking
     */
    public record UserRoleQuery(UserId userId, Role role) {}
}