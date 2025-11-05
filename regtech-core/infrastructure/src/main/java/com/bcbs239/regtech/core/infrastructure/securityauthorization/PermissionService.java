package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import java.util.Set;

/**
 * Service interface for permission validation and user authorization.
 * Implementations should integrate with JWT tokens and user management systems.
 */
public interface PermissionService {

    /**
     * Validate if the provided JWT token is valid and not expired.
     *
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    boolean isValidToken(String token);

    /**
     * Extract user permissions from JWT token.
     *
     * @param token JWT token containing user information
     * @return Set of permission strings, or null if token is invalid
     */
    Set<String> getUserPermissions(String token);

    /**
     * Extract bank ID from JWT token for multi-tenant authorization.
     *
     * @param token JWT token containing bank information
     * @return Bank ID string, or null if not available
     */
    String getBankId(String token);

    /**
     * Extract user ID from JWT token.
     *
     * @param token JWT token containing user information
     * @return User ID string, or null if not available
     */
    String getUserId(String token);

    /**
     * Check if user has a specific permission.
     *
     * @param token JWT token
     * @param permission Permission to check
     * @return true if user has the permission, false otherwise
     */
    default boolean hasPermission(String token, String permission) {
        Set<String> userPermissions = getUserPermissions(token);
        return userPermissions != null && userPermissions.contains(permission);
    }

    /**
     * Check if user has all required permissions.
     *
     * @param token JWT token
     * @param requiredPermissions Array of required permissions
     * @return true if user has all permissions, false otherwise
     */
    default boolean hasAllPermissions(String token, String... requiredPermissions) {
        Set<String> userPermissions = getUserPermissions(token);
        if (userPermissions == null) {
            return false;
        }
        
        for (String permission : requiredPermissions) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }
}

