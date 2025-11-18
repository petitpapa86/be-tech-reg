package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import com.bcbs239.regtech.core.domain.security.Authentication;

import java.util.Set;

/**
 * Simple authentication implementation for JWT-based authentication.
 * Moved to core module to avoid circular dependencies.
 */
public class SimpleAuthentication implements Authentication {

    private final String userId;
    private final String bankId;
    private final Set<String> permissions;
    private final Set<String> roles;

    public SimpleAuthentication(String userId, Set<String> permissions, Set<String> roles) {
        this.userId = userId;
        this.bankId = null;
        this.permissions = Set.copyOf(permissions);
        this.roles = Set.copyOf(roles);
    }

    public SimpleAuthentication(String userId, String bankId, Set<String> permissions, Set<String> roles) {
        this.userId = userId;
        this.bankId = bankId;
        this.permissions = Set.copyOf(permissions);
        this.roles = Set.copyOf(roles);
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Set<String> getRoles() {
        return roles;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getBankId() {
        return bankId;
    }
}