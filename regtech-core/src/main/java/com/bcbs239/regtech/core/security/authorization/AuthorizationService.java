package com.bcbs239.regtech.core.security.authorization;

import java.util.Set;
import java.util.Optional;

/**
 * Core authorization service that provides cross-module permission checking.
 * This allows any module to check user permissions without directly depending on IAM.
 */
public interface AuthorizationService {
    
    /**
     * Check if the current user has a specific permission
     */
    boolean hasPermission(String permission);
    
    /**
     * Check if the current user has any of the specified permissions
     */
    boolean hasAnyPermission(String... permissions);
    
    /**
     * Check if the current user has all of the specified permissions
     */
    boolean hasAllPermissions(String... permissions);
    
    /**
     * Check if the current user has a specific role
     */
    boolean hasRole(String role);
    
    /**
     * Check if the current user has any of the specified roles
     */
    boolean hasAnyRole(String... roles);
    
    /**
     * Get all permissions for the current user
     */
    Set<String> getCurrentUserPermissions();
    
    /**
     * Get all roles for the current user
     */
    Set<String> getCurrentUserRoles();
    
    /**
     * Get the current user's ID
     */
    Optional<String> getCurrentUserId();
    
    /**
     * Check if user has permission for a specific resource
     */
    boolean hasResourcePermission(String resourceType, String resourceId, String action);
    
    /**
     * Check if user can access data for a specific organization/tenant
     */
    boolean canAccessOrganization(String organizationId);
}