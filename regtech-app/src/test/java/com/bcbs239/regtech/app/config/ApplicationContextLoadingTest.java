package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.billing.infrastructure.config.BillingProperties;
import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import com.bcbs239.regtech.dataquality.infrastructure.config.DataQualityProperties;
import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionProperties;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the application context loads successfully
 * with all modules and their configuration files.
 * 
 * Tests that:
 * - Application starts successfully with all modules
 * - All configuration files are found and loaded
 * - All @ConfigurationProperties beans are created
 * 
 * Requirements: 1.1, 1.2, 6.1
 */
@SpringBootTest
@ActiveProfiles("development")
class ApplicationContextLoadingTest {

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
    void applicationContextLoadsSuccessfully() {
        assertNotNull(applicationContext, "Application context should load successfully");
        assertTrue(applicationContext.containsBean("ingestionProperties"), 
            "Application context should contain beans");
    }

    @Test
    void allModuleConfigurationFilesAreLoaded() {
        // Verify that all module configuration files are loaded by checking
        // that their @ConfigurationProperties beans are created
        
        assertNotNull(ingestionProperties, 
            "Ingestion configuration should be loaded from application-ingestion.yml");
        assertNotNull(dataQualityProperties, 
            "Data quality configuration should be loaded from application-data-quality.yml");
        assertNotNull(riskCalculationProperties, 
            "Risk calculation configuration should be loaded from application-risk-calculation.yml");
        assertNotNull(reportGenerationProperties, 
            "Report generation configuration should be loaded from application-report-generation.yml");
        assertNotNull(billingProperties, 
            "Billing configuration should be loaded from application-billing.yml");
        assertNotNull(iamProperties, 
            "IAM configuration should be loaded from application-iam.yml");
    }

    @Test
    void allConfigurationPropertiesBeansAreCreated() {
        // Verify that all @ConfigurationProperties beans are registered in the context
        
        assertTrue(applicationContext.containsBean("ingestionProperties"), 
            "IngestionProperties bean should be created");
        assertTrue(applicationContext.containsBean("dataQualityProperties"), 
            "DataQualityProperties bean should be created");
        assertTrue(applicationContext.containsBean("riskCalculationProperties"), 
            "RiskCalculationProperties bean should be created");
        assertTrue(applicationContext.containsBean("reportGenerationProperties"), 
            "ReportGenerationProperties bean should be created");
        assertTrue(applicationContext.containsBean("billingProperties"), 
            "BillingProperties bean should be created");
        assertTrue(applicationContext.containsBean("iAMProperties"), 
            "IAMProperties bean should be created");
    }

    @Test
    void sharedInfrastructureConfigurationIsLoaded() {
        // Verify that shared infrastructure configuration from root application.yml is loaded
        
        // Check datasource configuration
        String datasourceUrl = applicationContext.getEnvironment()
            .getProperty("spring.datasource.url");
        assertNotNull(datasourceUrl, "Datasource URL should be configured");
        
        // Check JPA configuration
        String ddlAuto = applicationContext.getEnvironment()
            .getProperty("spring.jpa.hibernate.ddl-auto");
        assertNotNull(ddlAuto, "JPA DDL auto should be configured");
        
        // Check Flyway configuration
        Boolean flywayEnabled = applicationContext.getEnvironment()
            .getProperty("spring.flyway.enabled", Boolean.class);
        assertNotNull(flywayEnabled, "Flyway enabled flag should be configured");
    }

    @Test
    void loggingConfigurationIsLoaded() {
        // Verify that logging configuration is loaded
        
        String loggingConfig = applicationContext.getEnvironment()
            .getProperty("logging.config");
        assertNotNull(loggingConfig, "Logging configuration file should be specified");
        
        String rootLogLevel = applicationContext.getEnvironment()
            .getProperty("logging.level.com.bcbs239.regtech");
        assertNotNull(rootLogLevel, "Root log level should be configured");
    }

    @Test
    void actuatorConfigurationIsLoaded() {
        // Verify that Spring Actuator configuration is loaded
        
        String exposedEndpoints = applicationContext.getEnvironment()
            .getProperty("management.endpoints.web.exposure.include");
        assertNotNull(exposedEndpoints, "Actuator endpoints should be configured");
        assertTrue(exposedEndpoints.contains("health"), 
            "Health endpoint should be exposed");
    }

    @Test
    void moduleSpecificConfigurationIsIsolated() {
        // Verify that each module has its own isolated configuration
        
        // Each module should have different storage prefixes
        String ingestionPrefix = ingestionProperties.getStorage().getS3().getPrefix();
        String dataQualityPrefix = dataQualityProperties.getStorage().getS3().getPrefix();
        String riskPrefix = riskCalculationProperties.getStorage().getS3().getPrefix();
        
        assertNotEquals(ingestionPrefix, dataQualityPrefix, 
            "Ingestion and data quality should have different S3 prefixes");
        assertNotEquals(ingestionPrefix, riskPrefix, 
            "Ingestion and risk calculation should have different S3 prefixes");
        assertNotEquals(dataQualityPrefix, riskPrefix, 
            "Data quality and risk calculation should have different S3 prefixes");
    }

    @Test
    void allModulesAreEnabled() {
        // Verify that all modules are enabled by default
        
        assertTrue(ingestionProperties.getEnabled(), 
            "Ingestion module should be enabled");
        assertTrue(dataQualityProperties.getEnabled(), 
            "Data quality module should be enabled");
        assertTrue(riskCalculationProperties.getEnabled(), 
            "Risk calculation module should be enabled");
        assertTrue(reportGenerationProperties.getEnabled(), 
            "Report generation module should be enabled");
        assertTrue(billingProperties.getEnabled(), 
            "Billing module should be enabled");
        assertTrue(iamProperties.getEnabled(), 
            "IAM module should be enabled");
    }

    @Test
    void asyncConfigurationIsLoadedForAllModules() {
        // Verify that async configuration is loaded for all modules that use it
        
        assertNotNull(ingestionProperties.getAsync(), 
            "Ingestion async configuration should be loaded");
        assertNotNull(dataQualityProperties.getAsync(), 
            "Data quality async configuration should be loaded");
        assertNotNull(riskCalculationProperties.getAsync(), 
            "Risk calculation async configuration should be loaded");
        assertNotNull(reportGenerationProperties.getAsync(), 
            "Report generation async configuration should be loaded");
    }

    @Test
    void storageConfigurationIsLoadedForAllModules() {
        // Verify that storage configuration is loaded for modules that use it
        
        assertNotNull(ingestionProperties.getStorage(), 
            "Ingestion storage configuration should be loaded");
        assertNotNull(dataQualityProperties.getStorage(), 
            "Data quality storage configuration should be loaded");
        assertNotNull(riskCalculationProperties.getStorage(), 
            "Risk calculation storage configuration should be loaded");
        assertNotNull(reportGenerationProperties.getS3(), 
            "Report generation S3 configuration should be loaded");
    }

    @Test
    void securityConfigurationIsLoadedFromRootConfig() {
        // Verify that security configuration is loaded from root application.yml
        
        assertNotNull(iamProperties.getSecurity(), 
            "Security configuration should be loaded");
        assertNotNull(iamProperties.getSecurity().getJwt(), 
            "JWT configuration should be loaded");
        assertNotNull(iamProperties.getSecurity().getPassword(), 
            "Password policy configuration should be loaded");
        assertNotNull(iamProperties.getSecurity().getPublicPaths(), 
            "Public paths configuration should be loaded");
    }

    @Test
    void eventProcessingConfigurationIsLoaded() {
        // Verify that event processing configuration is loaded from root application.yml
        
        Boolean outboxEnabled = applicationContext.getEnvironment()
            .getProperty("regtech.outbox.enabled", Boolean.class);
        Boolean inboxEnabled = applicationContext.getEnvironment()
            .getProperty("regtech.inbox.enabled", Boolean.class);
        
        assertNotNull(outboxEnabled, "Outbox enabled flag should be configured");
        assertNotNull(inboxEnabled, "Inbox enabled flag should be configured");
    }

    @Test
    void profileSpecificConfigurationIsApplied() {
        // Verify that development profile configuration is applied
        
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        assertTrue(activeProfiles.length > 0, "At least one profile should be active");
        assertEquals("development", activeProfiles[0], 
            "Development profile should be active");
        
        // Verify profile-specific values are applied
        // In development, storage type should be local
        String storageType = ingestionProperties.getStorage().getType();
        assertEquals("local", storageType, 
            "Development profile should use local storage");
    }
}
