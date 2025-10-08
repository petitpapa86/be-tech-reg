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
    
    public static UserRole create(String userId, Role role, String organizationId) {
        UserRole userRole = new UserRole();
        userRole.id = java.util.UUID.randomUUID().toString();
        userRole.userId = userId;
        userRole.role = role;
        userRole.organizationId = organizationId;
        userRole.active = true;
        return userRole;
    }
    
    public void activate() {
        this.active = true;
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Role getRole() { return role; }
    public String getOrganizationId() { return organizationId; }
    public boolean isActive() { return active; }
}