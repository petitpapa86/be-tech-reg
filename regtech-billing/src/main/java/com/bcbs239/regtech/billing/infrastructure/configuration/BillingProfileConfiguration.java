package com.bcbs239.regtech.billing.infrastructure.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

/**
 * Profile-specific configuration for the billing module.
 * Provides different beans and configurations based on the active Spring profile.
 */
@Configuration
public class BillingProfileConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(BillingProfileConfiguration.class);

    /**
     * Development profile configuration
     */
    @Configuration
    @Profile("dev")
    static class DevelopmentConfiguration {
        
        @Bean
        public BillingEnvironmentInfo billingEnvironmentInfo() {
            logger.info("Billing module running in DEVELOPMENT mode");
            return new BillingEnvironmentInfo(
                "development",
                true,  // debug mode
                false, // scheduled jobs disabled
                false, // notifications disabled
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
        public BillingEnvironmentInfo billingEnvironmentInfo() {
            logger.info("Billing module running in TEST mode");
            return new BillingEnvironmentInfo(
                "test",
                true,  // debug mode
                false, // scheduled jobs disabled
                false, // notifications disabled
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
        public BillingEnvironmentInfo billingEnvironmentInfo() {
            logger.info("Billing module running in PRODUCTION mode");
            return new BillingEnvironmentInfo(
                "production",
                false, // debug mode disabled
                true,  // scheduled jobs enabled
                true,  // notifications enabled
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
        public BillingEnvironmentInfo billingEnvironmentInfo() {
            logger.warn("Billing module running with DEFAULT profile - consider setting explicit profile");
            return new BillingEnvironmentInfo(
                "default",
                true,  // debug mode
                false, // scheduled jobs disabled
                false, // notifications disabled
                "Default configuration"
            );
        }
    }

    /**
     * Information about the current billing environment configuration
     */
    public record BillingEnvironmentInfo(
        String profileName,
        boolean debugMode,
        boolean scheduledJobsEnabled,
        boolean notificationsEnabled,
        String databaseInfo
    ) {
        
        public void logConfiguration() {
            logger.info("Billing Environment Configuration:");
            logger.info("  Profile: {}", profileName);
            logger.info("  Debug Mode: {}", debugMode);
            logger.info("  Scheduled Jobs: {}", scheduledJobsEnabled ? "ENABLED" : "DISABLED");
            logger.info("  Notifications: {}", notificationsEnabled ? "ENABLED" : "DISABLED");
            logger.info("  Database: {}", databaseInfo);
        }
    }
}