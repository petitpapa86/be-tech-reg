package com.bcbs239.regtech.core.infrastructure;

import com.bcbs239.regtech.core.infrastructure.securityauthorization.AuthorizationService;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityContext;
import com.bcbs239.regtech.core.infrastructure.securityauthorization.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class DefaultAuthorizationService implements AuthorizationService {

    @Override
    public boolean hasPermission(String permission) {
        return SecurityContext.hasPermission(permission);
    }

    @Override
    public boolean hasAnyPermission(String... permissions) {
        return SecurityContext.hasAnyPermission(permissions);
    }

    @Override
    public boolean hasAllPermissions(String... permissions) {
        return SecurityContext.hasAllPermissions(permissions);
    }

    @Override
    public boolean hasRole(String role) {
        Set<String> roles = SecurityContext.getCurrentUserPermissions();
        if (roles == null) return false;
        return roles.contains(role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        Set<String> current = SecurityContext.getCurrentUserPermissions();
        if (current == null) return false;
        for (String r : roles) {
            if (current.contains(r)) return true;
        }
        return false;
    }

    @Override
    public Set<String> getCurrentUserPermissions() {
        Set<String> perms = SecurityContext.getCurrentUserPermissions();
        return perms == null ? Set.of() : Set.copyOf(perms);
    }

    @Override
    public Set<String> getCurrentUserRoles() {
        // Roles are not modelled separately; return empty set or extract from permissions if available
        return Set.of();
    }

    @Override
    public Optional<String> getCurrentUserId() {
        return Optional.ofNullable(SecurityContext.getCurrentUserId());
    }

    @Override
    public boolean hasResourcePermission(String resourceType, String resourceId, String action) {
        // Default: if user has a permission composed as resourceType:action then allow
        String composite = resourceType + ":" + action;
        return hasPermission(composite) || hasAnyPermission(composite);
    }

    @Override
    public boolean canAccessOrganization(String organizationId) {
        // Delegate to SecurityUtils which has bank/org access helpers
        return SecurityUtils.canAccessBank(organizationId);
    }
}

