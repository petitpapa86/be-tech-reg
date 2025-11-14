package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import com.bcbs239.regtech.core.domain.security.Authentication;

import java.util.Set;

/**
 * Anonymous authentication for public endpoints.
 * Represents unauthenticated access with limited permissions.
 */
public class AnonymousAuthentication implements Authentication {

    private static final AnonymousAuthentication INSTANCE = new AnonymousAuthentication();

    public static AnonymousAuthentication instance() {
        return INSTANCE;
    }

    @Override
    public String getUserId() {
        return "anonymous";
    }

    @Override
    public Set<String> getPermissions() {
        return Set.of("public:read"); // Limited public permissions
    }

    @Override
    public Set<String> getRoles() {
        return Set.of("ANONYMOUS");
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }
}