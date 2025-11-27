package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionProperties;
import com.bcbs239.regtech.dataquality.infrastructure.config.DataQualityProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;
import com.bcbs239.regtech.billing.infrastructure.config.BillingProperties;
import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the application starts successfully
 * with no profile specified (default configuration).
 * 
 * Tests that:
 * - Application starts with default configuration
 * - All modules load with default settings
 * - Default values are applied correctly
 * 
 * Requirements: 7.1, 7.2
 */
@SpringBootTest
class DefaultProfileStartupTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IngestionProperties ingestionProperties;

    @Autowired
    private DataQualityProperties dataQualityProperties;

    @Autowired
    private RiskCalculationProperties riskCalculationProperties;

    @Autowired
    private ReportGenerationProperties reportGenerationProperties;

    @Autowired
    private BillingProperties billingProperties;

    @Autowired
    private IAMProperties iamProperties;

    @Test
    void applicationStartsWithDefaultProfile() {
        assertNotNull(applicationContext, "Application context should load with default profile");
        
        // When no profile is specified, Spring Boot uses "default" profile
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        // If no profile is explicitly set, activeProfiles may be empty or contain "default"
        assertTrue(activeProfiles.length == 0 || 
                   (activeProfiles.length == 1 && "default".equals(activeProfiles[0])),
            "No specific profile or default profile should be active");
    }

    @Test
    void allModulesLoadWithDefaultConfiguration() {
        // Verify all configuration properties beans are created
        assertNotNull(ingestionProperties, "Ingestion properties should be loaded");
        assertNotNull(dataQualityProperties, "Data quality properties should be loaded");
        assertNotNull(riskCalculationProperties, "Risk calculation properties should be loaded");
        assertNotNull(reportGenerationProperties, "Report generation properties should be loaded");
        assertNotNull(billingProperties, "Billing properties should be loaded");
        assertNotNull(iamProperties, "IAM properties should be loaded");
    }

    @Test
    void defaultStorageConfigurationIsApplied() {
        // Verify default storage configuration
        assertNotNull(ingestionProperties.getStorage(), "Ingestion storage should be configured");
        assertNotNull(dataQualityProperties.getStorage(), "Data quality storage should be configured");
        assertNotNull(riskCalculationProperties.getStorage(), "Risk calculation storage should be configured");
        assertNotNull(reportGenerationProperties.getS3(), "Report generation S3 should be configured");
    }

    @Test
    void defaultAsyncConfigurationIsApplied() {
        // Verify default async configuration
        assertNotNull(ingestionProperties.getAsync(), "Ingestion async should be configured");
        assertNotNull(dataQualityProperties.getAsync(), "Data quality async should be configured");
        assertNotNull(riskCalculationProperties.getAsync(), "Risk calculation async should be configured");
        assertNotNull(reportGenerationProperties.getAsync(), "Report generation async should be configured");
        
        // Verify default thread pool sizes are reasonable
        assertTrue(ingestionProperties.getAsync().getCorePoolSize() > 0,
            "Default core pool size should be positive");
        assertTrue(ingestionProperties.getAsync().getMaxPoolSize() > 0,
            "Default max pool size should be positive");
    }

    @Test
    void allModulesAreEnabledByDefault() {
        // Verify all modules are enabled by default
        assertTrue(ingestionProperties.getEnabled(), "Ingestion should be enabled by default");
        assertTrue(dataQualityProperties.getEnabled(), "Data quality should be enabled by default");
        assertTrue(riskCalculationProperties.getEnabled(), "Risk calculation should be enabled by default");
        assertTrue(reportGenerationProperties.getEnabled(), "Report generation should be enabled by default");
        assertTrue(billingProperties.getEnabled(), "Billing should be enabled by default");
        assertTrue(iamProperties.getEnabled(), "IAM should be enabled by default");
    }

    @Test
    void securityConfigurationIsLoaded() {
        // Verify security configuration is loaded
        assertNotNull(iamProperties.getSecurity(), "Security configuration should be loaded");
        assertNotNull(iamProperties.getSecurity().getJwt(), "JWT configuration should be loaded");
        assertNotNull(iamProperties.getSecurity().getPassword(), "Password policy should be loaded");
        assertNotNull(iamProperties.getSecurity().getPublicPaths(), "Public paths should be loaded");
        assertFalse(iamProperties.getSecurity().getPublicPaths().isEmpty(),
            "Public paths list should not be empty");
    }

    @Test
    void sharedInfrastructureConfigurationIsLoaded() {
        // Verify shared infrastructure configuration
        String datasourceUrl = applicationContext.getEnvironment()
            .getProperty("spring.datasource.url");
        assertNotNull(datasourceUrl, "Datasource URL should be configured");
        
        Boolean flywayEnabled = applicationContext.getEnvironment()
            .getProperty("spring.flyway.enabled", Boolean.class);
        assertNotNull(flywayEnabled, "Flyway should be configured");
    }
}
