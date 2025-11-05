package com.bcbs239.regtech.iam.infrastructure.configuration;

import java.time.Duration;

/**
 * Type-safe configuration for JWT settings.
 * Provides access to JWT secret and expiration settings.
 */
public record JwtConfiguration(
    String secret,
    Duration expiration
) {
    
    /**
     * Gets the JWT secret key
     */
    public String getSecret() {
        return secret;
    }
    
    /**
     * Gets the JWT expiration duration
     */
    public Duration getExpiration() {
        return expiration;
    }
    
    /**
     * Gets the JWT expiration in seconds
     */
    public long getExpirationSeconds() {
        return expiration.getSeconds();
    }
    
    /**
     * Validates JWT configuration
     */
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is required but not configured");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }
        if (secret.contains("your-jwt-secret") || secret.contains("placeholder")) {
            throw new IllegalStateException("JWT secret appears to be a placeholder value");
        }
        if (expiration == null || expiration.isZero() || expiration.isNegative()) {
            throw new IllegalStateException("JWT expiration must be positive");
        }
        if (expiration.toDays() > 30) {
            throw new IllegalStateException("JWT expiration should not exceed 30 days for security reasons");
        }
    }
    
    /**
     * Checks if JWT is configured for production use
     */
    public boolean isProductionReady() {
        try {
            validate();
            return !secret.contains("test") && !secret.contains("dev");
        } catch (IllegalStateException e) {
            return false;
        }
    }
}

