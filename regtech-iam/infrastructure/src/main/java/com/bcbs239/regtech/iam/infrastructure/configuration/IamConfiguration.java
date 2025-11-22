package com.bcbs239.regtech.iam.infrastructure.configuration;

import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * Main configuration class for the IAM module.
 * Enables configuration properties and provides configuration beans.
 */
@Configuration
@EnableConfigurationProperties({IamConfigurationProperties.class, IAMProperties.class})
public class IamConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(IamConfiguration.class);

    private final IamConfigurationProperties properties;
    private final Environment environment;

    public IamConfiguration(IamConfigurationProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfigurationOnStartup() {
        String[] activeProfiles = environment.getActiveProfiles();
        String profileInfo = activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default";
        
        logger.info("IAM module initialized with profiles: [{}]", profileInfo);
        logger.info("JWT expiration: {} seconds", properties.security().jwt().expiration());
        logger.info("Authorization cache: {}", properties.authorization().cache().enabled() ? "ENABLED" : "DISABLED");
        logger.info("Multi-tenant mode: {}", properties.authorization().multiTenant().enabled() ? "ENABLED" : "DISABLED");
        logger.info("Permissions strict mode: {}", properties.authorization().permissions().strictMode() ? "ENABLED" : "DISABLED");
    }

    /**
     * Provides access to JWT configuration
     */
    @Bean
    public JwtConfiguration jwtConfiguration() {
        var jwt = properties.security().jwt();
        return new JwtConfiguration(
            jwt.secret(),
            jwt.getExpirationDuration()
        );
    }

    /**
     * Provides access to password policy configuration
     */
    @Bean
    public PasswordPolicyConfiguration passwordPolicyConfiguration() {
        var password = properties.security().password();
        return new PasswordPolicyConfiguration(
            password.minLength(),
            password.requireUppercase(),
            password.requireLowercase(),
            password.requireDigits(),
            password.requireSpecialChars()
        );
    }

    /**
     * Provides access to OAuth2 configuration
     */
    @Bean
    public OAuth2Configuration oauth2Configuration() {
        var oauth2 = properties.security().oauth2();
        return new OAuth2Configuration(
            new OAuth2Configuration.GoogleConfig(
                oauth2.google().clientId(),
                oauth2.google().clientSecret()
            ),
            new OAuth2Configuration.FacebookConfig(
                oauth2.facebook().clientId(),
                oauth2.facebook().clientSecret()
            )
        );
    }

    /**
     * Provides access to authorization configuration
     */
    @Bean
    public AuthorizationConfiguration authorizationConfiguration() {
        var auth = properties.authorization();
        return new AuthorizationConfiguration(
            new AuthorizationConfiguration.CacheConfig(
                auth.cache().enabled(),
                auth.cache().getTtlDuration()
            ),
            new AuthorizationConfiguration.MultiTenantConfig(
                auth.multiTenant().enabled(),
                auth.multiTenant().defaultOrganization()
            ),
            new AuthorizationConfiguration.PermissionsConfig(
                auth.permissions().strictMode(),
                auth.permissions().auditEnabled()
            )
        );
    }
}

