package com.bcbs239.regtech.core.domain.security;

/**
 * Domain-level security context interface.
 * Provides access to authentication and authorization information.
 */
public interface SecurityContext {
    /**
     * Get the current authentication information.
     */
    Authentication getAuthentication();

    /**
     * Check if the current user has a specific permission.
     */
    boolean hasPermission(String permission);

    /**
     * Check if the current user has any of the specified permissions.
     */
    boolean hasAnyPermission(String... permissions);

    /**
     * Check if the current user has all of the specified permissions.
     */
    boolean hasAllPermissions(String... permissions);

    /**
     * Get the current user ID.
     */
    String getUserId();

    /**
     * Check if this is a system-level operation.
     */
    boolean isSystem();
}