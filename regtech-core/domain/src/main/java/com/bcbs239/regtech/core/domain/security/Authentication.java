package com.bcbs239.regtech.core.domain.security;

import java.util.Set;

/**
 * Domain-level authentication interface.
 * Represents the authentication information for a user.
 */
public interface Authentication {
    /**
     * Get the user ID of the authenticated user.
     */
    String getUserId();

    /**
     * Get the permissions granted to the authenticated user.
     */
    Set<String> getPermissions();

    /**
     * Get the roles assigned to the authenticated user.
     */
    Set<String> getRoles();

    /**
     * Check if the user is authenticated.
     */
    boolean isAuthenticated();
}