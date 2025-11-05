package com.bcbs239.regtech.iam.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Main configuration properties for the IAM module.
 * Maps to the 'iam' section in application-iam.yml
 */
@ConfigurationProperties(prefix = "iam")
public record IamConfigurationProperties(
    SecurityProperties security,
    AuthorizationProperties authorization
) {

    /**
     * Security configuration properties
     */
    public record SecurityProperties(
        JwtProperties jwt,
        PasswordProperties password,
        OAuth2Properties oauth2
    ) {}

    /**
     * JWT configuration properties
     */
    public record JwtProperties(
        String secret,
        long expiration
    ) {
        
        public Duration getExpirationDuration() {
            return Duration.ofSeconds(expiration);
        }
    }

    /**
     * Password policy configuration properties
     */
    public record PasswordProperties(
        int minLength,
        boolean requireUppercase,
        boolean requireLowercase,
        boolean requireDigits,
        boolean requireSpecialChars
    ) {}

    /**
     * OAuth2 configuration properties
     */
    public record OAuth2Properties(
        GoogleOAuth2Properties google,
        FacebookOAuth2Properties facebook
    ) {}

    /**
     * Google OAuth2 configuration
     */
    public record GoogleOAuth2Properties(
        String clientId,
        String clientSecret
    ) {}

    /**
     * Facebook OAuth2 configuration
     */
    public record FacebookOAuth2Properties(
        String clientId,
        String clientSecret
    ) {}

    /**
     * Authorization configuration properties
     */
    public record AuthorizationProperties(
        CacheProperties cache,
        MultiTenantProperties multiTenant,
        PermissionsProperties permissions
    ) {}

    /**
     * Cache configuration properties
     */
    public record CacheProperties(
        boolean enabled,
        long ttl
    ) {
        
        public Duration getTtlDuration() {
            return Duration.ofSeconds(ttl);
        }
    }

    /**
     * Multi-tenant configuration properties
     */
    public record MultiTenantProperties(
        boolean enabled,
        String defaultOrganization
    ) {}

    /**
     * Permissions configuration properties
     */
    public record PermissionsProperties(
        boolean strictMode,
        boolean auditEnabled
    ) {}
}