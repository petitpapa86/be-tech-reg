package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.iam.domain.users.Bcbs239Role;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.domain.users.UserRepository.UserOrgQuery;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserBankAssignmentEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Consolidated JPA repository for User aggregate operations using Spring Data JPA repositories.
 * Follows the established architecture patterns with proper domain/persistence separation.
 */
@Repository
public class JpaUserRepository implements com.bcbs239.regtech.iam.domain.users.UserRepository {

    private final ILogger asyncLogger;
    private final SpringDataUserRepository userRepository;
    private final SpringDataUserRoleRepository userRoleRepository;
    private final SpringDataUserBankAssignmentRepository userBankAssignmentRepository;

    @Autowired
    public JpaUserRepository(ILogger asyncLogger, SpringDataUserRepository userRepository, SpringDataUserRoleRepository userRoleRepository, SpringDataUserBankAssignmentRepository userBankAssignmentRepository) {
        this.asyncLogger = asyncLogger;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userBankAssignmentRepository = userBankAssignmentRepository;
    }


    /**
     * Closure to find user by email
     * Returns Maybe<User> to handle optional results functionally
     */
    @Override
    public Maybe<User> emailLookup(Email email) {
        try {
            return userRepository.findByEmail(email.getValue())
                    .map(UserEntity::toDomain)
                    .map(Maybe::some)
                    .orElse(Maybe.none());
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
            return userRepository.findById(userId.getValue())
                    .map(UserEntity::toDomain)
                    .map(Maybe::some)
                    .orElse(Maybe.none());
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
        try {
            // Check if user exists in database to determine if this is an update
            boolean isUpdate = userRepository.existsById(user.getId().getValue());
            
            UserEntity savedEntity;
            if (isUpdate) {
                // For updates, find existing entity and update its fields
                UserEntity existingEntity = userRepository.findById(user.getId().getValue())
                        .orElseThrow(() -> new IllegalStateException("User not found for update: " + user.getId().getValue()));
                
                // Update fields from domain
                existingEntity.setEmail(user.getEmail().getValue());
                existingEntity.setPasswordHash(user.getPassword().getHashedValue());
                existingEntity.setFirstName(user.getFirstName());
                existingEntity.setLastName(user.getLastName());
                existingEntity.setStatus(user.getStatus());
                existingEntity.setGoogleId(user.getGoogleId());
                existingEntity.setFacebookId(user.getFacebookId());
                existingEntity.setUpdatedAt(user.getUpdatedAt());
                
                // For bank assignments, we don't update them in this scenario
                // They are managed separately through other operations
                // Clearing the collection causes Hibernate to try setting user_id to null
                
                savedEntity = userRepository.save(existingEntity);
            } else {
                // For new entities, create fresh entity
                UserEntity entity = UserEntity.fromDomain(user);
                savedEntity = userRepository.save(entity);
            }
            
            return Result.success(UserId.fromString(savedEntity.getId()));
        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("Failed to save user", e, Map.of("user", user.getId(), "error", e.getMessage()));
            return Result.failure(ErrorDetail.of("USER_SAVE_FAILED", ErrorType.SYSTEM_ERROR, "Failed to save user: " + e.getMessage(), "user.save.failed"));
        }
    }

    /**
     * Closure to find all active roles for a user
     */
    @Override
    public List<UserRole> userRolesFinder(UserId userId) {
        try {
            return userRoleRepository.findByUserIdAndActiveTrue(userId.getValue())
                    .stream()
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
            return userRoleRepository.findByUserIdAndOrganizationIdAndActiveTrue(query.userId().getValue(), query.organizationId())
                    .stream()
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
        try {
            UserRoleEntity entity = UserRoleEntity.fromDomain(userRole);
            UserRoleEntity saved = userRoleRepository.save(entity);
            return Result.success(saved.getId());
        } catch (Exception e) {
            asyncLogger.asyncStructuredErrorLog("Failed to save user role", e, Map.of("userRole", userRole, "error", e.getMessage()));
            return Result.failure(ErrorDetail.of("USER_ROLE_SAVE_FAILED", ErrorType.SYSTEM_ERROR, "Failed to save user role: " + e.getMessage(), "user.role.save.failed"));
        }
    }

    /**
     * Closure to check if user has a specific role
     */
    public Function<UserRoleQuery, Boolean> userRoleChecker() {
        return query -> {
            try {
                return userRoleRepository.existsByUserIdAndRoleAndActiveTrue(query.userId().getValue(), query.role());
            } catch (Exception e) {
                return false;
            }
        };
    }

    /**
     * Traditional method for complex queries that don't fit the closure pattern
     */
    public List<User> findByStatus(UserStatus status) {
        return userRepository.findByStatus(status)
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
    public record UserRoleQuery(UserId userId, Bcbs239Role role) {}
}

