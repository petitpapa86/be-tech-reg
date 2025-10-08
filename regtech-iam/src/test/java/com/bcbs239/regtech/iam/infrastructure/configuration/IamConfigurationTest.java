package com.bcbs239.regtech.iam.infrastructure.configuration;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for IAM configuration classes validation without requiring Spring context.
 */
class IamConfigurationTest {

    @Test
    void jwtConfiguration_shouldValidateCorrectly() {
        // Valid configuration
        JwtConfiguration validConfig = new JwtConfiguration(
            "a-very-secure-jwt-secret-key-that-is-long-enough-for-production-use",
            Duration.ofHours(1)
        );
        
        assertThat(validConfig.getSecret()).isNotBlank();
        assertThat(validConfig.getExpiration()).isEqualTo(Duration.ofHours(1));
        assertThat(validConfig.getExpirationSeconds()).isEqualTo(3600);
        
        // Should not throw exception
        validConfig.validate();
        assertThat(validConfig.isProductionReady()).isTrue();
    }

    @Test
    void jwtConfiguration_shouldFailValidationForShortSecret() {
        JwtConfiguration invalidConfig = new JwtConfiguration(
            "short-secret",
            Duration.ofHours(1)
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32 characters");
    }

    @Test
    void jwtConfiguration_shouldFailValidationForPlaceholderSecret() {
        JwtConfiguration invalidConfig = new JwtConfiguration(
            "your-jwt-secret-key-here-placeholder-value",
            Duration.ofHours(1)
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("placeholder");
    }

    @Test
    void jwtConfiguration_shouldFailValidationForTooLongExpiration() {
        JwtConfiguration invalidConfig = new JwtConfiguration(
            "a-very-secure-jwt-secret-key-that-is-long-enough-for-production-use",
            Duration.ofDays(31)
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("should not exceed 30 days");
    }

    @Test
    void passwordPolicyConfiguration_shouldValidateCorrectly() {
        PasswordPolicyConfiguration validConfig = new PasswordPolicyConfiguration(
            12, true, true, true, true
        );
        
        assertThat(validConfig.getMinLength()).isEqualTo(12);
        assertThat(validConfig.isUppercaseRequired()).isTrue();
        assertThat(validConfig.isLowercaseRequired()).isTrue();
        assertThat(validConfig.areDigitsRequired()).isTrue();
        assertThat(validConfig.areSpecialCharsRequired()).isTrue();
        
        // Should not throw exception
        validConfig.validate();
        assertThat(validConfig.getStrengthLevel()).isEqualTo(PasswordPolicyConfiguration.PasswordStrength.STRONG);
    }

    @Test
    void passwordPolicyConfiguration_shouldValidatePasswords() {
        PasswordPolicyConfiguration config = new PasswordPolicyConfiguration(
            8, true, true, true, true
        );
        
        // Valid password
        assertThat(config.isValidPassword("Password123!")).isTrue();
        
        // Invalid passwords
        assertThat(config.isValidPassword("short")).isFalse(); // too short
        assertThat(config.isValidPassword("password123!")).isFalse(); // no uppercase
        assertThat(config.isValidPassword("PASSWORD123!")).isFalse(); // no lowercase
        assertThat(config.isValidPassword("Password!")).isFalse(); // no digits
        assertThat(config.isValidPassword("Password123")).isFalse(); // no special chars
    }

    @Test
    void passwordPolicyConfiguration_shouldFailValidationForTooShortMinLength() {
        PasswordPolicyConfiguration invalidConfig = new PasswordPolicyConfiguration(
            6, true, true, true, true
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 8 characters");
    }

    @Test
    void oauth2Configuration_shouldValidateCorrectly() {
        OAuth2Configuration validConfig = new OAuth2Configuration(
            new OAuth2Configuration.GoogleConfig("valid-google-client-id", "valid-google-client-secret"),
            new OAuth2Configuration.FacebookConfig("valid-facebook-client-id", "valid-facebook-client-secret")
        );
        
        assertThat(validConfig.getGoogle().isConfigured()).isTrue();
        assertThat(validConfig.getFacebook().isConfigured()).isTrue();
        assertThat(validConfig.hasConfiguredProviders()).isTrue();
        
        // Should not throw exception
        validConfig.validate();
    }

    @Test
    void oauth2Configuration_shouldHandleEmptyConfiguration() {
        OAuth2Configuration emptyConfig = new OAuth2Configuration(
            new OAuth2Configuration.GoogleConfig("", ""),
            new OAuth2Configuration.FacebookConfig("", "")
        );
        
        assertThat(emptyConfig.getGoogle().isConfigured()).isFalse();
        assertThat(emptyConfig.getFacebook().isConfigured()).isFalse();
        assertThat(emptyConfig.hasConfiguredProviders()).isFalse();
        
        // Should not throw exception for empty config
        emptyConfig.validate();
    }

    @Test
    void authorizationConfiguration_shouldValidateCorrectly() {
        AuthorizationConfiguration validConfig = new AuthorizationConfiguration(
            new AuthorizationConfiguration.CacheConfig(true, Duration.ofMinutes(5)),
            new AuthorizationConfiguration.MultiTenantConfig(true, "default-org"),
            new AuthorizationConfiguration.PermissionsConfig(true, true)
        );
        
        assertThat(validConfig.getCache().isEnabled()).isTrue();
        assertThat(validConfig.getCache().getTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(validConfig.getMultiTenant().isEnabled()).isTrue();
        assertThat(validConfig.getMultiTenant().getDefaultOrganization()).isEqualTo("default-org");
        assertThat(validConfig.getPermissions().isStrictMode()).isTrue();
        assertThat(validConfig.getPermissions().isAuditEnabled()).isTrue();
        
        // Should not throw exception
        validConfig.validate();
    }

    @Test
    void authorizationConfiguration_shouldFailValidationForInvalidCacheTtl() {
        AuthorizationConfiguration invalidConfig = new AuthorizationConfiguration(
            new AuthorizationConfiguration.CacheConfig(true, Duration.ofHours(2)), // Too long
            new AuthorizationConfiguration.MultiTenantConfig(false, ""),
            new AuthorizationConfiguration.PermissionsConfig(false, false)
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("should not exceed 60 minutes");
    }

    @Test
    void authorizationConfiguration_shouldFailValidationForMissingDefaultOrg() {
        AuthorizationConfiguration invalidConfig = new AuthorizationConfiguration(
            new AuthorizationConfiguration.CacheConfig(false, Duration.ofMinutes(5)),
            new AuthorizationConfiguration.MultiTenantConfig(true, ""), // Empty default org
            new AuthorizationConfiguration.PermissionsConfig(false, false)
        );
        
        assertThatThrownBy(invalidConfig::validate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Default organization is required");
    }
}