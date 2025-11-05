package com.bcbs239.regtech.core.domain.security;

/**
 * Domain service interface for authorization operations.
 */
public interface IAuthorizationService {

    boolean hasPermission(String userId, String permission);

    boolean hasRole(String userId, String role);

    boolean isAuthorized(String userId, String resource, String action);
}

