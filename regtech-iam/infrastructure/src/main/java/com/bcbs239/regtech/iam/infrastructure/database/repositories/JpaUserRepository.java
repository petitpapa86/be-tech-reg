package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.security.authorization.Role;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.domain.users.UserRepository.UserOrgQuery;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserBankAssignmentEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserRoleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Consolidated JPA repository for User aggregate operations using EntityManager and closures.
 * Follows the established architecture patterns with proper domain/persistence separation.
 */
@Repository
public class JpaUserRepository implements com.bcbs239.regtech.iam.domain.users.UserRepository {

    private final ILogger asyncLogger;

    @PersistenceContext
    private EntityManager entityManager;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public JpaUserRepository(ILogger asyncLogger, EntityManager entityManager, TransactionTemplate transactionTemplate) {
        this.asyncLogger = asyncLogger;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }


    /**
     * Closure to find user by email
     * Returns Maybe<User> to handle optional results functionally
     */
    @Override
    public Maybe<User> emailLookup(Email email) {
        try {
            UserEntity entity = entityManager.createQuery(
                "SELECT u FROM UserEntity u WHERE u.email = :email", UserEntity.class)
                .setParameter("email", email.getValue())
                .getSingleResult();
            return Maybe.some(entity.toDomain());
        } catch (NoResultException e) {
            return Maybe.none();
        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("Failed to lookup user by email", e,
                    Map.of(
                            "email", email.getValue(),
                            "error", e.getMessage()
                    ));

            return Maybe.none();
        }
    }

    /**
     * Closure to load user by ID
     * Returns Result<User> for functional error handling
     */
    @Override
    public Maybe<User> userLoader(UserId userId) {
        try {
            UserEntity entity = entityManager.find(UserEntity.class, userId.getValue());
            if (entity == null) {
                return Maybe.none();
            }
            return Maybe.some(entity.toDomain());
        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("Failed to load user by id", e, Map.of("userId", userId.getValue(), "error", e.getMessage()));
            return Maybe.none();
        }
    }

    /**
     * Closure to save user
     * Returns Result<UserId> for functional error handling
     */
    @Override
    public Result<UserId> userSaver(User user) {
        // Ensure persistence happens inside a real transaction
        return transactionTemplate.execute(status -> {
            try {
                UserEntity entity = UserEntity.fromDomain(user);
                entity.setId(null);
                entityManager.persist(entity);
                for (UserBankAssignmentEntity assignment : entity.getBankAssignments()) {
                    assignment.setUserId(entity.getId());
                    assignment.setVersion(null);
                    entityManager.persist(assignment);
                }
                entityManager.flush();
                return Result.success(user.getId());
            } catch (Exception e) {
                asyncLogger.asyncStructuredErrorLog("Failed to save user", e, Map.of("user", user.getId(), "error", e.getMessage()));
                return Result.failure(ErrorDetail.of("USER_SAVE_FAILED", ErrorType.SYSTEM_ERROR, "Failed to save user: " + e.getMessage(), "user.save.failed"));
            }
        });
    }

    /**
     * Closure to find all active roles for a user
     */
    @Override
    public List<UserRole> userRolesFinder(UserId userId) {
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
            asyncLogger.asyncStructuredErrorLog("Failed to find user roles", e, Map.of("userId", userId.getValue(), "error", e.getMessage()));
            return List.of();
        }
    }

    /**
     * Closure to find roles for a user in a specific organization
     */
    @Override
    public List<UserRole> userOrgRolesFinder(UserOrgQuery query) {
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
            asyncLogger.asyncStructuredErrorLog("Failed to find user org roles", e, Map.of("userId", query.userId().getValue(), "organizationId", query.organizationId(), "error", e.getMessage()));
            return List.of();
        }
    }

    /**
     * Closure to save user role
     */
    @Override
    public Result<String> userRoleSaver(UserRole userRole) {
        return transactionTemplate.execute(status -> {
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
                asyncLogger.asyncStructuredErrorLog("Failed to save user role", e, Map.of("userRole", userRole, "error", e.getMessage()));
                return Result.failure(ErrorDetail.of("USER_ROLE_SAVE_FAILED", ErrorType.SYSTEM_ERROR, "Failed to save user role: " + e.getMessage(), "user.role.save.failed"));
            }
        });
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



    // Note: UserOrgQuery comes from the UserRepository nested record type

    @Override
    public Result<JwtToken> tokenGenerator(User user, String jwtSecretKey) {
        try {
            // Create JWT token with user information
            return JwtToken.generate(
                user,
                jwtSecretKey,
                java.time.Duration.ofHours(24)
            );
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("TOKEN_GENERATION_FAILED", ErrorType.SYSTEM_ERROR, "Failed to generate JWT token: " + e.getMessage(), "token.generation.failed"));
        }
    }

    /**
     * Query record for user role checking
     */
    public record UserRoleQuery(UserId userId, Role role) {}
}

