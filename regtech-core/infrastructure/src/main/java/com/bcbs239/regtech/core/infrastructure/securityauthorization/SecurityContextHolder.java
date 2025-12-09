package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import com.bcbs239.regtech.core.domain.security.SecurityContext;

import java.util.concurrent.Callable;

/**
 * Security context holder using Java 21 Scoped Values.
 * Provides thread-safe, immutable security context management.
 */
public class SecurityContextHolder {

    /**
     * Scoped value for holding the security context.
     * Scoped values are immutable and automatically cleaned up when the scope exits.
     */
    public static final ScopedValue<SecurityContext> SECURITY_CONTEXT =
        ScopedValue.newInstance();

    /**
     * Get the current security context.
     * Returns null if no context is available.
     */
    public static SecurityContext getContext() {
        return SECURITY_CONTEXT.orElse(null);
    }

    /**
     * Get the current security context, throwing an exception if none is available.
     */
    public static SecurityContext requireContext() {
        return SECURITY_CONTEXT.orElseThrow(() ->
            new IllegalStateException("No security context available"));
    }

    /**
     * Execute code with a specific security context.
     * The context is available only within the callable's execution.
     */
    public static <T> T runWithContext(SecurityContext context, Callable<T> callable)
            throws Exception {
        return ScopedValue.where(SECURITY_CONTEXT, context).call(() -> callable.call());
    }

    /**
     * Execute code with a specific security context.
     * The context is available only within the runnable's execution.
     */
    public static void runWithContext(SecurityContext context, Runnable runnable) {
        try {
            ScopedValue.where(SECURITY_CONTEXT, context).run(runnable);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}