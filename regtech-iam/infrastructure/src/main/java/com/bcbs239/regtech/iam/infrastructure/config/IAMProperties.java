package com.bcbs239.regtech.iam.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration properties for IAM module
 * Bound from application.yml (security section) and application-iam.yml
 * Requirements: 6.2, 6.5, 10.3, 11.1
 */
@Data
@Validated
@ConfigurationProperties(prefix = "iam")
public class IAMProperties {

    @NotNull(message = "IAM enabled flag must be specified")
    private Boolean enabled = true;

    @NotNull(message = "Security configuration must be specified")
    private SecurityProperties security = new SecurityProperties();

    @NotNull(message = "Authorization configuration must be specified")
    private AuthorizationProperties authorization = new AuthorizationProperties();

    @NotNull(message = "User management configuration must be specified")
    private UserManagementProperties userManagement = new UserManagementProperties();

    @NotNull(message = "Session configuration must be specified")
    private SessionProperties session = new SessionProperties();

    @NotNull(message = "Token cleanup configuration must be specified")
    private TokenCleanupProperties tokenCleanup = new TokenCleanupProperties();

    /**
     * Security configuration (JWT, OAuth2, public paths)
     * This is shared configuration from root application.yml
     */
    @Data
    public static class SecurityProperties {
        @NotNull(message = "JWT configuration must be specified")
        private JwtProperties jwt = new JwtProperties();

        @NotNull(message = "Password configuration must be specified")
        private PasswordProperties password = new PasswordProperties();

        @NotNull(message = "OAuth2 configuration must be specified")
        private OAuth2Properties oauth2 = new OAuth2Properties();

        @NotNull(message = "Public paths must be specified")
        private List<String> publicPaths = List.of();

        /**
         * JWT configuration
         */
        @Data
        public static class JwtProperties {
            @NotBlank(message = "JWT secret must be specified")
            private String secret;

            @Min(value = 1, message = "JWT expiration must be at least 1 second")
            private int expiration = 86400; // 24 hours (deprecated, use access-token-expiration-minutes)
            
            @Min(value = 1, message = "Access token expiration must be at least 1 minute")
            private int accessTokenExpirationMinutes = 15; // 15 minutes
            
            @Min(value = 1, message = "Refresh token expiration must be at least 1 day")
            private int refreshTokenExpirationDays = 7; // 7 days
        }

        /**
         * Password policy configuration
         */
        @Data
        public static class PasswordProperties {
            @Min(value = 8, message = "Minimum password length must be at least 8")
            private int minLength = 12;

            private boolean requireUppercase = true;
            private boolean requireLowercase = true;
            private boolean requireDigits = true;
            private boolean requireSpecialChars = true;
        }

        /**
         * OAuth2 providers configuration
         */
        @Data
        public static class OAuth2Properties {
            private GoogleProperties google = new GoogleProperties();
            private FacebookProperties facebook = new FacebookProperties();

            @Data
            public static class GoogleProperties {
                private String clientId;
                private String clientSecret;
            }

            @Data
            public static class FacebookProperties {
                private String clientId;
                private String clientSecret;
            }
        }
    }

    /**
     * Authorization configuration
     */
    @Data
    public static class AuthorizationProperties {
        @NotNull(message = "Cache configuration must be specified")
        private CacheProperties cache = new CacheProperties();

        @NotNull(message = "Multi-tenant configuration must be specified")
        private MultiTenantProperties multiTenant = new MultiTenantProperties();

        @NotNull(message = "Permissions configuration must be specified")
        private PermissionsProperties permissions = new PermissionsProperties();

        @Data
        public static class CacheProperties {
            private boolean enabled = true;

            @Min(value = 0, message = "Cache TTL must be non-negative")
            private int ttl = 300; // 5 minutes
        }

        @Data
        public static class MultiTenantProperties {
            private boolean enabled = true;

            @NotBlank(message = "Default organization must be specified")
            private String defaultOrganization = "default-org";
        }

        @Data
        public static class PermissionsProperties {
            private boolean strictMode = true;
            private boolean auditEnabled = true;
        }
    }

    /**
     * User management configuration
     */
    @Data
    public static class UserManagementProperties {
        @Min(value = 1, message = "Password reset token expiration must be at least 1 second")
        private int passwordResetTokenExpiration = 3600; // 1 hour

        @Min(value = 1, message = "Email verification token expiration must be at least 1 second")
        private int emailVerificationTokenExpiration = 86400; // 24 hours

        @NotNull(message = "Lockout configuration must be specified")
        private LockoutProperties lockout = new LockoutProperties();

        @Data
        public static class LockoutProperties {
            private boolean enabled = true;

            @Min(value = 1, message = "Max failed attempts must be at least 1")
            private int maxFailedAttempts = 5;

            @Min(value = 1, message = "Lockout duration must be at least 1 second")
            private int lockoutDuration = 1800; // 30 minutes
        }
    }

    /**
     * Session management configuration
     */
    @Data
    public static class SessionProperties {
        @Min(value = 1, message = "Max concurrent sessions must be at least 1")
        private int maxConcurrentSessions = 3;

        @Min(value = 1, message = "Session timeout must be at least 1 second")
        private int timeout = 3600; // 1 hour

        @Min(value = 1, message = "Remember-me validity must be at least 1 second")
        private int rememberMeValidity = 2592000; // 30 days
    }

    /**
     * Token cleanup configuration
     * Requirements: 6.5
     */
    @Data
    public static class TokenCleanupProperties {
        private boolean enabled = true;

        @NotBlank(message = "Cleanup cron schedule must be specified")
        private String cron = "0 0 2 * * ?"; // Daily at 2 AM

        @Min(value = 1, message = "Retention days must be at least 1")
        private int retentionDays = 30; // Keep expired tokens for 30 days before deletion
    }
}
