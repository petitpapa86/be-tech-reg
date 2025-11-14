package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.Set;

/**
 * Service for loading role permissions.
 * Supports both enum-based (deprecated) and database-driven permission loading.
 */
public interface RolePermissionService {

    /**
     * Load permissions for a given role name.
     * First tries database, falls back to Bcbs239Role enum for backward compatibility.
     *
     * @param roleName The role name (e.g., "SYSTEM_ADMIN")
     * @return Set of permission strings
     */
    Result<Set<String>> loadPermissions(String roleName);

    /**
     * Load all available roles.
     *
     * @return Set of role names
     */
    Result<Set<String>> loadAllRoles();
}