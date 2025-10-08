package com.bcbs239.regtech.iam.infrastructure.security;

import com.bcbs239.regtech.core.security.authorization.AuthorizationService;
import com.bcbs239.regtech.core.security.authorization.Role;
import com.bcbs239.regtech.iam.domain.users.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * IAM implementation of the authorization service.
 * This is the authoritative source for user permissions and roles.
 */
@Service
public class IamAuthorizationService implements AuthorizationService {
    
    private final UserRoleRepository userRoleRepository;
    
    public IamAuthorizationService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }
    
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
        return getCurrentUserId()
            .map(this::getUserPermissions)
            .orElse(Set.of());
    }
    
    @Override
    public Set<String> getCurrentUserRoles() {
        return getCurrentUserId()
            .map(this::getUserRoles)
            .orElse(Set.of());
    }
    
    @Override
    public Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return Optional.of(authentication.getName());
        }
        return Optional.empty();
    }
    
    @Override
    public boolean hasResourcePermission(String resourceType, String resourceId, String action) {
        // Check if user has general permission for this action
        String permission = resourceType + ":" + action;
        if (!hasPermission(permission)) {
            return false;
        }
        
        // Additional resource-specific checks can be added here
        // For example, checking if user owns the resource or has organization access
        return true;
    }
    
    @Override
    public boolean canAccessOrganization(String organizationId) {
        return getCurrentUserId()
            .map(userId -> userRoleRepository.findByUserIdAndOrganizationId(userId, organizationId))
            .map(roles -> !roles.isEmpty())
            .orElse(false);
    }
    
    /**
     * Get all permissions for a specific user
     */
    private Set<String> getUserPermissions(String userId) {
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        return userRoles.stream()
            .map(UserRole::getRole)
            .flatMap(role -> role.getPermissions().stream())
            .collect(Collectors.toSet());
    }
    
    /**
     * Get all roles for a specific user
     */
    private Set<String> getUserRoles(String userId) {
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        return userRoles.stream()
            .map(userRole -> userRole.getRole().name())
            .collect(Collectors.toSet());
    }
}