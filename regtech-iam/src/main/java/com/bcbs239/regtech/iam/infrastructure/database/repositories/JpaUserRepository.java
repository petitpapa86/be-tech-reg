package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.security.authorization.Role;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.application.authenticate.OAuth2UserInfo;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserRoleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

/**
 * Consolidated JPA repository for User aggregate operations using EntityManager and closures.
 * Follows the established architecture patterns with proper domain/persistence separation.
 */
@Repository
@Transactional
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
                
                if (entity.getId() == null) {
                    entityManager.persist(entity);
                } else {
                    entity = entityManager.merge(entity);
                }
                
                entityManager.flush();
                return Result.success(UserId.fromString(entity.getId()).getValue().get());
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("USER_SAVE_FAILED",
                    "Failed to save user: " + e.getMessage(), "error.user.saveFailed"));
            }
        };
    }

    /**
     * Closure to save OAuth user (find or create)
     * Returns Result<User> for functional error handling
     */
    public Function<OAuth2UserInfo, Result<User>> saveOAuthUser() {
        return userInfo -> {
            try {
                // Check if user already exists
                Maybe<User> existingUser = emailLookup().apply(userInfo.email());
                if (existingUser.isPresent()) {
                    User user = existingUser.getValue();
                    // Update OAuth ID if not set
                    if (userInfo.externalId() != null && user.getGoogleId() == null) {
                        user.setGoogleId(userInfo.externalId());
                        Result<UserId> saveResult = userSaver().apply(user);
                        if (saveResult.isFailure()) {
                            return Result.failure(saveResult.getError().get());
                        }
                    }
                    return Result.success(user);
                }

                // Create new user from OAuth info
                User newUser = User.createOAuth(
                    userInfo.email(),
                    userInfo.firstName(),
                    userInfo.lastName(),
                    userInfo.externalId()
                );

                Result<UserId> saveResult = userSaver().apply(newUser);
                if (saveResult.isFailure()) {
                    return Result.failure(saveResult.getError().get());
                }

                return Result.success(newUser);
            } catch (Exception e) {
                return Result.failure(ErrorDetail.of("OAUTH_USER_SAVE_FAILED",
                    "Failed to save OAuth user: " + e.getMessage(),
                    "error.oauth.user.saveFailed"));
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

    /**
     * Query record for user role checking
     */
    public record UserRoleQuery(UserId userId, Role role) {}
}