package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import com.bcbs239.regtech.core.domain.security.Authentication;
import com.bcbs239.regtech.core.domain.security.SecurityContext;

import java.util.Arrays;

/**
 * Default implementation of the SecurityContext interface.
 */
public class DefaultSecurityContext implements SecurityContext {

    private final Authentication authentication;

    public DefaultSecurityContext(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public Authentication getAuthentication() {
        return authentication;
    }

    @Override
    public boolean hasPermission(String permission) {
        return authentication.getPermissions().contains(permission);
    }

    @Override
    public boolean hasAnyPermission(String... permissions) {
        return Arrays.stream(permissions)
            .anyMatch(this::hasPermission);
    }

    @Override
    public boolean hasAllPermissions(String... permissions) {
        return Arrays.stream(permissions)
            .allMatch(this::hasPermission);
    }

    @Override
    public String getUserId() {
        return authentication.getUserId();
    }

    @Override
    public boolean isSystem() {
        return "system".equals(getUserId());
    }
}