package com.bcbs239.regtech.iam.infrastructure.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that required environment variables and configuration are present
 * for the IAM module, especially in production environments.
 */
@Component
@Profile("prod")
public class IamEnvironmentValidator {

    private static final Logger logger = LoggerFactory.getLogger(IamEnvironmentValidator.class);

    private final Environment environment;
    private final IamConfigurationProperties properties;

    public IamEnvironmentValidator(Environment environment, IamConfigurationProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateEnvironment() {
        logger.info("Validating IAM module environment configuration...");
        
        List<String> errors = new ArrayList<>();
        
        // Validate required environment variables
        validateRequiredEnvironmentVariables(errors);
        
        // Validate configuration properties
        validateConfigurationProperties(errors);
        
        // Validate JWT configuration
        validateJwtConfiguration(errors);
        
        // Validate OAuth2 configuration
        validateOAuth2Configuration(errors);
        
        if (!errors.isEmpty()) {
            logger.error("IAM module environment validation failed:");
            errors.forEach(error -> logger.error("  - {}", error));
            throw new IllegalStateException("IAM module environment validation failed. See logs for details.");
        }
        
        logger.info("IAM module environment validation completed successfully");
    }

    private void validateRequiredEnvironmentVariables(List<String> errors) {
        String[] requiredVars = {
            "JWT_SECRET",
            "DB_PASSWORD",
            "DB_HOST",
            "DB_NAME",
            "DB_USERNAME"
        };
        
        for (String var : requiredVars) {
            String value = environment.getProperty(var);
            if (value == null || value.trim().isEmpty()) {
                errors.add("Required environment variable not set: " + var);
            }
        }
    }

    private void validateConfigurationProperties(List<String> errors) {
        try {
            // Validate each configuration section
            if (properties.security() != null) {
                validateSecurityConfiguration(errors);
            }
            
            if (properties.authorization() != null) {
                validateAuthorizationConfiguration(errors);
            }
            
        } catch (Exception e) {
            errors.add("Configuration validation error: " + e.getMessage());
        }
    }

    private void validateJwtConfiguration(List<String> errors) {
        try {
            String jwtSecret = properties.security().jwt().secret();
            long expiration = properties.security().jwt().expiration();
            
            if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
                errors.add("JWT secret is not configured");
            } else if (jwtSecret.contains("your-jwt-secret") || jwtSecret.contains("placeholder")) {
                errors.add("JWT secret appears to be a placeholder value in production");
            } else if (jwtSecret.length() < 32) {
                errors.add("JWT secret should be at least 32 characters long for security");
            } else if (jwtSecret.contains("dev") || jwtSecret.contains("test")) {
                errors.add("JWT secret appears to be a development/test value in production");
            }
            
            if (expiration <= 0) {
                errors.add("JWT expiration must be positive");
            } else if (expiration > 86400) { // 24 hours
                errors.add("JWT expiration should not exceed 24 hours in production for security");
            }
            
        } catch (Exception e) {
            errors.add("JWT configuration validation error: " + e.getMessage());
        }
    }

    private void validateOAuth2Configuration(List<String> errors) {
        try {
            var oauth2 = properties.security().oauth2();
            
            // OAuth2 providers are optional, but if configured, they should be valid
            if (oauth2.google().clientId() != null && !oauth2.google().clientId().isBlank()) {
                if (oauth2.google().clientId().contains("dev") || oauth2.google().clientId().contains("test")) {
                    errors.add("Google OAuth2 client ID appears to be a development/test value in production");
                }
                if (oauth2.google().clientSecret() == null || oauth2.google().clientSecret().isBlank()) {
                    errors.add("Google OAuth2 client secret is required when client ID is configured");
                }
            }
            
            if (oauth2.facebook().clientId() != null && !oauth2.facebook().clientId().isBlank()) {
                if (oauth2.facebook().clientId().contains("dev") || oauth2.facebook().clientId().contains("test")) {
                    errors.add("Facebook OAuth2 client ID appears to be a development/test value in production");
                }
                if (oauth2.facebook().clientSecret() == null || oauth2.facebook().clientSecret().isBlank()) {
                    errors.add("Facebook OAuth2 client secret is required when client ID is configured");
                }
            }
            
        } catch (Exception e) {
            errors.add("OAuth2 configuration validation error: " + e.getMessage());
        }
    }

    private void validateSecurityConfiguration(List<String> errors) {
        try {
            var password = properties.security().password();
            if (password.minLength() < 8) {
                errors.add("Password minimum length should be at least 8 characters in production");
            }
            
            // Ensure strong password policy in production
            if (!password.requireUppercase() && !password.requireLowercase() && 
                !password.requireDigits() && !password.requireSpecialChars()) {
                errors.add("At least one password requirement should be enabled in production");
            }
            
        } catch (Exception e) {
            errors.add("Security configuration validation error: " + e.getMessage());
        }
    }

    private void validateAuthorizationConfiguration(List<String> errors) {
        try {
            var auth = properties.authorization();
            
            if (auth.cache().enabled()) {
                if (auth.cache().ttl() <= 0) {
                    errors.add("Authorization cache TTL must be positive when caching is enabled");
                }
                if (auth.cache().ttl() > 3600) { // 1 hour
                    errors.add("Authorization cache TTL should not exceed 1 hour in production for security");
                }
            }
            
            if (auth.multiTenant().enabled()) {
                if (auth.multiTenant().defaultOrganization() == null || 
                    auth.multiTenant().defaultOrganization().isBlank()) {
                    errors.add("Default organization is required when multi-tenancy is enabled");
                }
            }
            
        } catch (Exception e) {
            errors.add("Authorization configuration validation error: " + e.getMessage());
        }
    }
}