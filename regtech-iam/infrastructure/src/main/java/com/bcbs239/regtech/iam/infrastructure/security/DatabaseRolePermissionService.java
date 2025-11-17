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

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of RolePermissionService.
 * Loads permissions from database.
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

            // No permissions found in database
            logger.warn("No permissions found in database for role {}", roleName);
            return Result.success(Set.of());

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

            // No roles found in database
            logger.warn("No roles found in database");
            return Result.success(Set.of());

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
}