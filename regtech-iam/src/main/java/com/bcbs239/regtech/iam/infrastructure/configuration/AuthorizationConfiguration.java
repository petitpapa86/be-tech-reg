package com.bcbs239.regtech.iam.infrastructure.configuration;

import java.time.Duration;

/**
 * Type-safe configuration for authorization settings.
 * Defines caching, multi-tenancy, and permissions configurations.
 */
public record AuthorizationConfiguration(
    CacheConfig cache,
    MultiTenantConfig multiTenant,
    PermissionsConfig permissions
) {
    
    /**
     * Cache configuration for authorization data
     */
    public record CacheConfig(
        boolean enabled,
        Duration ttl
    ) {
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public Duration getTtl() {
            return ttl;
        }
        
        public long getTtlSeconds() {
            return ttl.getSeconds();
        }
        
        public void validate() {
            if (enabled) {
                if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                    throw new IllegalStateException("Cache TTL must be positive when caching is enabled");
                }
                if (ttl.toMinutes() > 60) {
                    throw new IllegalStateException("Cache TTL should not exceed 60 minutes for security reasons");
                }
                if (ttl.getSeconds() < 30) {
                    throw new IllegalStateException("Cache TTL should be at least 30 seconds to be effective");
                }
            }
        }
    }
    
    /**
     * Multi-tenant configuration
     */
    public record MultiTenantConfig(
        boolean enabled,
        String defaultOrganization
    ) {
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public String getDefaultOrganization() {
            return defaultOrganization;
        }
        
        public void validate() {
            if (enabled) {
                if (defaultOrganization == null || defaultOrganization.isBlank()) {
                    throw new IllegalStateException("Default organization is required when multi-tenancy is enabled");
                }
                if (defaultOrganization.length() > 100) {
                    throw new IllegalStateException("Default organization name is too long");
                }
                // Basic validation for organization name format
                if (!defaultOrganization.matches("^[a-zA-Z0-9\\-_]+$")) {
                    throw new IllegalStateException("Default organization name contains invalid characters");
                }
            }
        }
    }
    
    /**
     * Permissions configuration
     */
    public record PermissionsConfig(
        boolean strictMode,
        boolean auditEnabled
    ) {
        
        public boolean isStrictMode() {
            return strictMode;
        }
        
        public boolean isAuditEnabled() {
            return auditEnabled;
        }
        
        public void validate() {
            // No specific validation needed for permissions config
            // Both strict mode and audit are optional features
        }
    }
    
    /**
     * Gets cache configuration
     */
    public CacheConfig getCache() {
        return cache;
    }
    
    /**
     * Gets multi-tenant configuration
     */
    public MultiTenantConfig getMultiTenant() {
        return multiTenant;
    }
    
    /**
     * Gets permissions configuration
     */
    public PermissionsConfig getPermissions() {
        return permissions;
    }
    
    /**
     * Validates authorization configuration
     */
    public void validate() {
        if (cache != null) {
            cache.validate();
        }
        if (multiTenant != null) {
            multiTenant.validate();
        }
        if (permissions != null) {
            permissions.validate();
        }
    }
}