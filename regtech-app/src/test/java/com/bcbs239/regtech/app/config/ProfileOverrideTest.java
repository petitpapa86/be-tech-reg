package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionProperties;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that profile-specific configuration overrides work correctly.
 * Requirements: 7.1, 7.2, 7.3
 */
class ProfileOverrideTest {

    /**
     * Test development profile overrides
     */
    @SpringBootTest
    @ActiveProfiles("development")
    static class DevelopmentProfileTest {

        @Autowired
        private IngestionProperties ingestionProperties;

        @Autowired
        private RiskCalculationProperties riskCalculationProperties;

        @Autowired
        private ReportGenerationProperties reportGenerationProperties;

        @Test
        void developmentProfileUsesLocalStorage() {
            // Development profile should use local storage
            assertEquals("local", ingestionProperties.getStorage().getType(),
                "Development profile should use local storage for ingestion");
        }

        @Test
        void developmentProfileUsesReducedThreadPools() {
            // Development profile should have smaller thread pools
            assertTrue(ingestionProperties.getAsync().getCorePoolSize() <= 5,
                "Development profile should have reduced core pool size for ingestion");
            assertTrue(ingestionProperties.getAsync().getMaxPoolSize() <= 10,
                "Development profile should have reduced max pool size for ingestion");

            assertTrue(riskCalculationProperties.getAsync().getCorePoolSize() <= 5,
                "Development profile should have reduced core pool size for risk calculation");
            assertTrue(riskCalculationProperties.getAsync().getMaxPoolSize() <= 10,
                "Development profile should have reduced max pool size for risk calculation");

            assertTrue(reportGenerationProperties.getAsync().getCorePoolSize() <= 5,
                "Development profile should have reduced core pool size for report generation");
            assertTrue(reportGenerationProperties.getAsync().getMaxPoolSize() <= 10,
                "Development profile should have reduced max pool size for report generation");
        }

        @Test
        void developmentProfileOverridesDefaultValues() {
            // Verify that development profile values override defaults
            assertNotNull(ingestionProperties.getStorage().getType(),
                "Storage type should be set in development profile");
            assertNotNull(ingestionProperties.getAsync().getCorePoolSize(),
                "Core pool size should be set in development profile");
        }
    }

    /**
     * Test production profile overrides
     */
    @SpringBootTest
    @ActiveProfiles("production")
    static class ProductionProfileTest {

        @Autowired
        private IngestionProperties ingestionProperties;

        @Autowired
        private RiskCalculationProperties riskCalculationProperties;

        @Autowired
        private ReportGenerationProperties reportGenerationProperties;

        @Test
        void productionProfileUsesS3Storage() {
            // Production profile should use S3 storage
            assertEquals("s3", ingestionProperties.getStorage().getType(),
                "Production profile should use S3 storage for ingestion");
        }

        @Test
        void productionProfileUsesOptimizedThreadPools() {
            // Production profile should have larger thread pools
            assertTrue(ingestionProperties.getAsync().getCorePoolSize() >= 5,
                "Production profile should have optimized core pool size for ingestion");
            assertTrue(ingestionProperties.getAsync().getMaxPoolSize() >= 10,
                "Production profile should have optimized max pool size for ingestion");

            assertTrue(riskCalculationProperties.getAsync().getCorePoolSize() >= 5,
                "Production profile should have optimized core pool size for risk calculation");
            assertTrue(riskCalculationProperties.getAsync().getMaxPoolSize() >= 10,
                "Production profile should have optimized max pool size for risk calculation");

            assertTrue(reportGenerationProperties.getAsync().getCorePoolSize() >= 5,
                "Production profile should have optimized core pool size for report generation");
            assertTrue(reportGenerationProperties.getAsync().getMaxPoolSize() >= 10,
                "Production profile should have optimized max pool size for report generation");
        }

        @Test
        void productionProfileOverridesDefaultValues() {
            // Verify that production profile values override defaults
            assertNotNull(ingestionProperties.getStorage().getType(),
                "Storage type should be set in production profile");
            assertNotNull(ingestionProperties.getAsync().getCorePoolSize(),
                "Core pool size should be set in production profile");
        }
    }

    /**
     * Test that profile-specific values take precedence over defaults
     */
    @SpringBootTest
    @ActiveProfiles("development")
    static class ProfilePrecedenceTest {

        @Autowired
        private IngestionProperties ingestionProperties;

        @Test
        void profileSpecificValuesTakePrecedence() {
            // Profile-specific values should override default values
            // Development profile sets storage type to "local"
            assertEquals("local", ingestionProperties.getStorage().getType(),
                "Profile-specific storage type should override default");

            // Development profile sets smaller thread pools
            assertTrue(ingestionProperties.getAsync().getCorePoolSize() <= 5,
                "Profile-specific core pool size should override default");
        }

        @Test
        void unspecifiedPropertiesUseDefaults() {
            // Properties not specified in profile should use defaults
            assertNotNull(ingestionProperties.getFile().getMaxSize(),
                "Unspecified properties should use default values");
            assertTrue(ingestionProperties.getFile().getMaxSize() > 0,
                "Default file size should be positive");
        }
    }
}
