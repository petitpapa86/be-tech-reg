package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.Set;

/**
 * Service for loading role permissions from database.
 */
public interface RolePermissionService {

    /**
     * Load permissions for a given role name from database.
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