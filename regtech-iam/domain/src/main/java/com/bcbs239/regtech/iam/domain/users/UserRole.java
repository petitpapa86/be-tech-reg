package com.bcbs239.regtech.iam.domain.users;

/**
 * User role entity that links users to their roles and permissions.
 */
public class UserRole {
    
    private String id;
    private String userId;
    private Bcbs239Role role;
    private String organizationId; // For multi-tenant support
    private boolean active;
    
    // Private constructor for JPA
    private UserRole() {}
    
    public static UserRole create(UserId userId, Bcbs239Role role, String organizationId) {
        UserRole userRole = new UserRole();
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
        return create(userId, bcbs239Role, organizationId);
    }
    
    public void activate() {
        this.active = true;
    }
    
    public void deactivate() {
        this.active = false;
    }
    
    /**
     * Get the BCBS239 business role
     */
    public Bcbs239Role getBcbs239Role() {
        return role;
    }
    
    /**
     * Check if this is a BCBS239 role (always true for this domain)
     */
    public boolean isBcbs239Role() {
        return true;
    }
    
    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Bcbs239Role getRole() { return role; }
    public String getOrganizationId() { return organizationId; }
    public boolean isActive() { return active; }
}

