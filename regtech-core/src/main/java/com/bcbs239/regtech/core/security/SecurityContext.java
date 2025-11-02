package com.bcbs239.regtech.core.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

/**
 * Utility class for accessing security context information within request scope.
 * Provides easy access to user permissions, bank ID, and other security-related data.
 */
public class SecurityContext {

    private static final String USER_PERMISSIONS_ATTR = "userPermissions";
    private static final String AUTH_TOKEN_ATTR = "authToken";
    private static final String BANK_ID_ATTR = "bankId";
    private static final String USER_ID_ATTR = "userId";

    /**
     * Get current user's permissions from request context.
     */
    @SuppressWarnings("unchecked")
    public static Set<String> getCurrentUserPermissions() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return (Set<String>) request.getAttribute(USER_PERMISSIONS_ATTR);
        }
        return null;
    }

    /**
     * Get current user's auth token from request context.
     */
    public static String getCurrentAuthToken() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return (String) request.getAttribute(AUTH_TOKEN_ATTR);
        }
        return null;
    }

    /**
     * Get current user's bank ID from request context.
     */
    public static String getCurrentBankId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return (String) request.getAttribute(BANK_ID_ATTR);
        }
        return null;
    }

    /**
     * Get current user ID from request context.
     */
    public static String getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return (String) request.getAttribute(USER_ID_ATTR);
        }
        return null;
    }

    /**
     * Check if current user has a specific permission.
     */
    public static boolean hasPermission(String permission) {
        Set<String> permissions = getCurrentUserPermissions();
        return permissions != null && permissions.contains(permission);
    }

    /**
     * Check if current user has all specified permissions.
     */
    public static boolean hasAllPermissions(String... permissions) {
        Set<String> userPermissions = getCurrentUserPermissions();
        if (userPermissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if current user has any of the specified permissions.
     */
    public static boolean hasAnyPermission(String... permissions) {
        Set<String> userPermissions = getCurrentUserPermissions();
        if (userPermissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set security context attributes (used by filters).
     */
    public static void setSecurityContext(String authToken, Set<String> permissions, 
                                        String bankId, String userId) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            request.setAttribute(AUTH_TOKEN_ATTR, authToken);
            request.setAttribute(USER_PERMISSIONS_ATTR, permissions);
            request.setAttribute(BANK_ID_ATTR, bankId);
            request.setAttribute(USER_ID_ATTR, userId);
        }
    }

    /**
     * Clear security context (used by filters).
     */
    public static void clearSecurityContext() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            request.removeAttribute(AUTH_TOKEN_ATTR);
            request.removeAttribute(USER_PERMISSIONS_ATTR);
            request.removeAttribute(BANK_ID_ATTR);
            request.removeAttribute(USER_ID_ATTR);
        }
    }

    /**
     * Get current HTTP request from Spring's RequestContextHolder.
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}