package com.bcbs239.regtech.iam.domain.users;

import com.bcbs239.regtech.core.security.authorization.Role;

/**
 * User role entity that links users to their roles and permissions.
 */
public class UserRole {
    
    private String id;
    private String userId;
    private Role role;
    private String organizationId; // For multi-tenant support
    private boolean active;
    
    // Private constructor for JPA
    private UserRole() {}
    
    public static UserRole create(UserId userId, Role role, String organizationId) {
        UserRole userRole = new UserRole();
        userRole.id = java.util.UUID.randomUUID().toString();
        userRole.userId = userId.getValue();
        userRole.role = role;
        userRole.organizationId = organizationId;
        userRole.active = true;
        return userRole;
    }
    
    /**
     * Create UserRole from BCBS239 business role
     */
    public static UserRole createFromBcbs239Role(UserId userId, Bcbs239Role bcbs239Role, String organizationId) {
        Role coreRole = RoleMapping.toCoreRole(bcbs239Role);
        return create(userId, coreRole, organizationId);
    }
    
    public void activate() {
        this.active = true;
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    /**
     * Get the BCBS239 business role if applicable
     */
    public Bcbs239Role getBcbs239Role() {
        if (RoleMapping.isBcbs239Role(role)) {
            return RoleMapping.fromCoreRole(role);
        }
        throw new IllegalStateException("Role " + role + " is not a BCBS239 role");
    }
    
    /**
     * Check if this is a BCBS239 role
     */
    public boolean isBcbs239Role() {
        return RoleMapping.isBcbs239Role(role);
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Role getRole() { return role; }
    public String getOrganizationId() { return organizationId; }
    public boolean isActive() { return active; }
}