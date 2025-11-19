package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import com.bcbs239.regtech.core.domain.security.AuthorizationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Aspect to handle @RequiresPermission annotations.
 * Provides declarative authorization checking.
 */
@Aspect
@Component
public class AuthorizationAspect {
    
    private final AuthorizationService authorizationService;
    
    public AuthorizationAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }
    
    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        
        // Check required permissions (must have ALL)
        String[] requiredPermissions = requiresPermission.value();
        if (requiredPermissions.length > 0) {
            if (!authorizationService.hasAllPermissions(requiredPermissions)) {
                throw new AccessDeniedException("Insufficient permissions: " + String.join(", ", requiredPermissions));
            }
        }
        
        // Check alternative permissions (must have ANY)
        String[] anyOfPermissions = requiresPermission.anyOf();
        if (anyOfPermissions.length > 0) {
            if (!authorizationService.hasAnyPermission(anyOfPermissions)) {
                throw new AccessDeniedException("Missing any of required permissions: " + String.join(", ", anyOfPermissions));
            }
        }
        
        // Check resource-based permissions
        String resourceType = requiresPermission.resourceType();
        String action = requiresPermission.action();
        if (!resourceType.isEmpty() && !action.isEmpty()) {
            // Extract resource ID from method parameters (simplified example)
            Object[] args = joinPoint.getArgs();
            String resourceId = args.length > 0 ? args[0].toString() : "";
            
            if (!authorizationService.hasResourcePermission(resourceType, resourceId, action)) {
                throw new AccessDeniedException("Insufficient permissions for resource: " + resourceType + ":" + resourceId);
            }
        }
        
        return joinPoint.proceed();
    }
}

