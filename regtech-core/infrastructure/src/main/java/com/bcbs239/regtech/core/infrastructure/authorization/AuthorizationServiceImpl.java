package com.bcbs239.regtech.core.infrastructure.authorization;

import com.bcbs239.regtech.core.domain.security.AuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of AuthorizationService that integrates with Spring Security.
 * This provides a basic authorization service that can be extended by modules.
 * Note: This is not registered as a bean - modules should provide their own implementations.
 */
public class AuthorizationServiceImpl implements AuthorizationService {

    @Override
    public boolean hasPermission(String permission) {
        return getCurrentUserPermissions().contains(permission);
    }

    @Override
    public boolean hasAnyPermission(String... permissions) {
        Set<String> userPermissions = getCurrentUserPermissions();
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllPermissions(String... permissions) {
        Set<String> userPermissions = getCurrentUserPermissions();
        for (String permission : permissions) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        Set<String> userRoles = getCurrentUserRoles();
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getCurrentUserPermissions() {
        // Extract permissions from Spring Security authorities
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("PERMISSION_"))
                .map(authority -> authority.substring("PERMISSION_".length()))
                .collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    public Set<String> getCurrentUserRoles() {
        // Extract roles from Spring Security authorities
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    public Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return Optional.ofNullable(authentication.getName());
        }
        return Optional.empty();
    }

    @Override
    public boolean hasResourcePermission(String resourceType, String resourceId, String action) {
        // Basic implementation - check for resource-specific permission
        String requiredPermission = resourceType + ":" + action;
        return hasPermission(requiredPermission);
    }

    @Override
    public boolean canAccessOrganization(String organizationId) {
        // Basic implementation - allow access if user has admin role
        // This should be extended by specific modules
        return hasRole("ADMIN") || hasRole("MANAGER");
    }
}

