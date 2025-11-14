package com.bcbs239.regtech.iam.infrastructure.security;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.RolePermissionService;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.SpringDataRolePermissionRepository;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.SpringDataRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of RolePermissionService.
 * Loads permissions from database with fallback to enum for backward compatibility.
 */
@Service
public class DatabaseRolePermissionService implements RolePermissionService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseRolePermissionService.class);

    private final SpringDataRoleRepository roleRepository;
    private final SpringDataRolePermissionRepository rolePermissionRepository;

    public DatabaseRolePermissionService(
            SpringDataRoleRepository roleRepository,
            SpringDataRolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public Result<Set<String>> loadPermissions(String roleName) {
        try {
            // First try to load from database
            Set<String> permissions = rolePermissionRepository.findPermissionsByRoleName(roleName.toUpperCase());

            if (!permissions.isEmpty()) {
                logger.debug("Loaded {} permissions for role {} from database", permissions.size(), roleName);
                return Result.success(permissions);
            }

            // Fallback to enum for backward compatibility (can be removed once all roles are migrated)
            logger.warn("No permissions found in database for role {}, falling back to enum", roleName);
            return loadPermissionsFromEnum(roleName);

        } catch (Exception e) {
            logger.error("Error loading permissions for role {}: {}", roleName, e.getMessage());
            return Result.failure(ErrorDetail.of(
                "PERMISSION_LOAD_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to load permissions for role: " + roleName,
                "permission.load.failed"
            ));
        }
    }

    @Override
    public Result<Set<String>> loadAllRoles() {
        try {
            // Load from database first
            Set<String> roles = roleRepository.findAll().stream()
                    .map(role -> role.getName().toLowerCase())
                    .collect(Collectors.toSet());

            if (!roles.isEmpty()) {
                logger.debug("Loaded {} roles from database", roles.size());
                return Result.success(roles);
            }

            // Fallback to enum for backward compatibility
            logger.warn("No roles found in database, falling back to enum");
            return loadRolesFromEnum();

        } catch (Exception e) {
            logger.error("Error loading all roles: {}", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "ROLES_LOAD_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to load available roles",
                "roles.load.failed"
            ));
        }
    }

    /**
     * Fallback method to load permissions from enum (for backward compatibility).
     * TODO: Remove this method once all roles are migrated to database.
     */
    @Deprecated
    private Result<Set<String>> loadPermissionsFromEnum(String roleName) {
        try {
            // Import here to avoid circular dependency issues
            com.bcbs239.regtech.iam.domain.users.Bcbs239Role role =
                com.bcbs239.regtech.iam.domain.users.Bcbs239Role.valueOf(roleName.toUpperCase());
            Set<String> permissions = role.getPermissions();

            logger.debug("Loaded {} permissions for role {} from enum", permissions.size(), roleName);
            return Result.success(permissions);

        } catch (IllegalArgumentException e) {
            logger.warn("Unknown role: {}, returning empty permissions", roleName);
            return Result.success(Set.of());
        }
    }

    /**
     * Fallback method to load roles from enum (for backward compatibility).
     * TODO: Remove this method once all roles are migrated to database.
     */
    @Deprecated
    private Result<Set<String>> loadRolesFromEnum() {
        try {
            Set<String> roles = new HashSet<>();
            for (com.bcbs239.regtech.iam.domain.users.Bcbs239Role role :
                 com.bcbs239.regtech.iam.domain.users.Bcbs239Role.values()) {
                roles.add(role.name().toLowerCase());
            }

            return Result.success(roles);

        } catch (Exception e) {
            logger.error("Error loading roles from enum: {}", e.getMessage());
            return Result.failure(ErrorDetail.of(
                "ENUM_ROLES_LOAD_FAILED",
                ErrorType.SYSTEM_ERROR,
                "Failed to load roles from enum",
                "enum.roles.load.failed"
            ));
        }
    }
}