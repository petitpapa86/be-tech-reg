package com.bcbs239.regtech.iam.infrastructure.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Profile-specific configuration for the IAM module.
 * Provides different beans and configurations based on the active Spring profile.
 */
@Configuration
public class IamProfileConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(IamProfileConfiguration.class);

    /**
     * Development profile configuration
     */
    @Configuration
    @Profile("dev")
    static class DevelopmentConfiguration {
        
        @Bean
        public IamEnvironmentInfo iamEnvironmentInfo() {
            logger.info("IAM module running in DEVELOPMENT mode");
            return new IamEnvironmentInfo(
                "development",
                true,  // debug mode
                false, // strict security disabled
                false, // multi-tenancy disabled
                "H2 in-memory database"
            );
        }
    }

    /**
     * Test profile configuration
     */
    @Configuration
    @Profile("test")
    static class TestConfiguration {
        
        @Bean
        public IamEnvironmentInfo iamEnvironmentInfo() {
            logger.info("IAM module running in TEST mode");
            return new IamEnvironmentInfo(
                "test",
                true,  // debug mode
                false, // strict security disabled
                false, // multi-tenancy disabled
                "H2 in-memory database (test)"
            );
        }
    }

    /**
     * Production profile configuration
     */
    @Configuration
    @Profile("prod")
    static class ProductionConfiguration {
        
        @Bean
        public IamEnvironmentInfo iamEnvironmentInfo() {
            logger.info("IAM module running in PRODUCTION mode");
            return new IamEnvironmentInfo(
                "production",
                false, // debug mode disabled
                true,  // strict security enabled
                true,  // multi-tenancy enabled
                "PostgreSQL database"
            );
        }
    }

    /**
     * Default profile configuration (fallback)
     */
    @Configuration
    @Profile("default")
    static class DefaultConfiguration {
        
        @Bean
        public IamEnvironmentInfo iamEnvironmentInfo() {
            logger.warn("IAM module running with DEFAULT profile - consider setting explicit profile");
            return new IamEnvironmentInfo(
                "default",
                true,  // debug mode
                false, // strict security disabled
                false, // multi-tenancy disabled
                "Default configuration"
            );
        }
    }

    /**
     * Information about the current IAM environment configuration
     */
    public record IamEnvironmentInfo(
        String profileName,
        boolean debugMode,
        boolean strictSecurityEnabled,
        boolean multiTenancyEnabled,
        String databaseInfo
    ) {
        
        public void logConfiguration() {
            logger.info("IAM Environment Configuration:");
            logger.info("  Profile: {}", profileName);
            logger.info("  Debug Mode: {}", debugMode);
            logger.info("  Strict Security: {}", strictSecurityEnabled ? "ENABLED" : "DISABLED");
            logger.info("  Multi-Tenancy: {}", multiTenancyEnabled ? "ENABLED" : "DISABLED");
            logger.info("  Database: {}", databaseInfo);
        }
    }
}