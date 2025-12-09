package com.bcbs239.regtech.iam.domain.users;

import lombok.Getter;

/**
 * User role entity that links users to their roles and permissions.
 * Now uses database-driven role names instead of deprecated enum.
 */
@Getter
public class UserRole {

    // Getters
    private String id;
    private String userId;
    private String roleName; // Database-driven role name
    private String organizationId; // For multi-tenant support
    private boolean active;

    // Private constructor for JPA
    private UserRole() {}

    public static UserRole create(UserId userId, String roleName, String organizationId) {
        UserRole userRole = new UserRole();
        userRole.userId = userId.getValue();
        userRole.roleName = roleName;
        userRole.organizationId = organizationId;
        userRole.active = true;
        return userRole;
    }    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * Get the role name
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Check if this is a BCBS239 role (always true for this domain)
     */
    public boolean isBcbs239Role() {
        return true;
    }

}


