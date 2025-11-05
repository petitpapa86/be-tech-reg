package com.bcbs239.regtech.core.domain.security;

/**
 * Domain service interface for security context operations.
 */
public interface ISecurityContext {

    String getCurrentUserId();

    String getCurrentUserName();

    boolean isAuthenticated();

    boolean hasAuthority(String authority);
}

