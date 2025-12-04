package com.bcbs239.regtech.iam.infrastructure.database.repositories;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.valueobjects.Email;
import com.bcbs239.regtech.iam.domain.users.*;
import com.bcbs239.regtech.iam.infrastructure.database.entities.RoleEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserEntity;
import com.bcbs239.regtech.iam.infrastructure.database.entities.UserRoleEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Consolidated JPA repository for User aggregate operations using Spring Data JPA repositories.
 * Follows the established architecture patterns with proper domain/persistence separation.
 */
@Repository
public class JpaUserRepository implements com.bcbs239.regtech.iam.domain.users.UserRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaUserRepository.class);
    private final SpringDataUserRepository userRepository;
    private final SpringDataUserRoleRepository userRoleRepository;
    private final SpringDataUserBankAssignmentRepository userBankAssignmentRepository;
    private final SpringDataRoleRepository roleRepository;

    @Autowired
    public JpaUserRepository(SpringDataUserRepository userRepository, SpringDataUserRoleRepository userRoleRepository, SpringDataUserBankAssignmentRepository userBankAssignmentRepository, SpringDataRoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userBankAssignmentRepository = userBankAssignmentRepository;
        this.roleRepository = roleRepository;
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
            log.error("Failed to lookup user by email; details={}", Map.of(
                    "email", email.getValue(),
                    "error", e.getMessage()
            ), e);

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
            return userRepository.findById(userId.getUUID())
                    .map(UserEntity::toDomain)
                    .map(Maybe::some)
                    .orElse(Maybe.none());
        } catch (Exception e) {
            log.error("Failed to load user by id; details={}", Map.of("userId", userId.getValue(), "error", e.getMessage()), e);
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
            boolean isUpdate = userRepository.existsById(user.getId().getUUID());

            UserEntity savedEntity;
            if (isUpdate) {
                // For updates, find existing entity and update its fields
                UserEntity existingEntity = userRepository.findById(user.getId().getUUID())
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
            
            return Result.success(new UserId(savedEntity.getId()));
        } catch (Exception e) {
            log.error("Failed to save user; details={}", Map.of("user", user.getId(), "error", e.getMessage()), e);
            return Result.failure(ErrorDetail.of("USER_SAVE_FAILED", ErrorType.SYSTEM_ERROR, "Failed to save user: " + e.getMessage(), "user.save.failed"));
        }
    }

    /**
     * Closure to find all active roles for a user
     */
    @Override
    public List<UserRole> userRolesFinder(UserId userId) {
        try {
            return userRoleRepository.findByUserIdAndActiveTrue(userId.getUUID())
                    .stream()
                    .map(entity -> convertToDomain(entity))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to find user roles; details={}", Map.of("userId", userId.getValue(), "error", e.getMessage()), e);
            return List.of();
        }
    }

    /**
     * Closure to find roles for a user in a specific organization
     */
    @Override
    public List<UserRole> userOrgRolesFinder(UserOrgQuery query) {
        try {
            return userRoleRepository.findByUserIdAndOrganizationIdAndActiveTrue(query.userId().getUUID(), query.organizationId())
                    .stream()
                    .map(entity -> convertToDomain(entity))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to find user org roles; details={}", Map.of("userId", query.userId().getValue(), "organizationId", query.organizationId(), "error", e.getMessage()), e);
            return List.of();
        }
    }

    /**
     * Convert UserRoleEntity to UserRole domain object by loading role from database
     */
    private UserRole convertToDomain(UserRoleEntity entity) {
        try {
            if (entity.getRoleId() == null) {
                throw new IllegalStateException("Role ID is null for UserRoleEntity: " + entity.getId());
            }

            RoleEntity roleEntity = roleRepository.findById(entity.getRoleId())
                    .orElseThrow(() -> new IllegalStateException("Role not found for ID: " + entity.getRoleId()));

            String roleName = roleEntity.getName();
            if (roleName == null) {
                throw new IllegalStateException("Role name is null for role ID: " + entity.getRoleId());
            }

            return UserRole.create(
                new UserId(entity.getUserId()),
                roleName,
                entity.getOrganizationId()
            );
        } catch (Exception e) {
            log.error("Failed to convert UserRoleEntity to domain; details={}",
                Map.of("entityId", entity.getId(), "roleId", entity.getRoleId(), "error", e.getMessage()), e);
            throw new RuntimeException("Failed to convert UserRoleEntity to domain: " + entity.getId(), e);
        }
    }

    /**
     * Closure to save a user role
     */
    @Override
    public Result<String> userRoleSaver(UserRole userRole) {
        try {
            UserRoleEntity entity = UserRoleEntity.fromDomain(userRole);

            // Set roleId by looking up the role from database using role name
            String roleId = roleRepository.findByName(userRole.getRoleName())
                    .map(roleEntity -> roleEntity.getId())
                    .orElseThrow(() -> new IllegalStateException("Role not found for name: " + userRole.getRoleName()));

            entity.setRoleId(roleId);

            UserRoleEntity saved = userRoleRepository.save(entity);
            return Result.success(saved.getId());
        } catch (Exception e) {
            log.error("Failed to save user role; details={}", Map.of("userRole", userRole, "error", e.getMessage()), e);
            return Result.failure(ErrorDetail.of("USER_ROLE_SAVE_FAILED", ErrorType.SYSTEM_ERROR, "Failed to save user role: " + e.getMessage(), "user.role.save.failed"));
        }
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
}
