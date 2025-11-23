package com.bcbs239.regtech.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that all module configurations load correctly.
 * This test validates backward compatibility after configuration reorganization.
 * 
 * Requirements: 9.1, 9.2
 */
@SpringBootTest
@ActiveProfiles("development")
class ConfigurationLoadingTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationContextLoadsSuccessfully() {
        // If we reach this point, the application context loaded successfully
        assertNotNull(applicationContext, "Application context should be loaded");
        assertTrue(applicationContext.getBeanDefinitionCount() > 0, 
            "Application context should contain beans");
    }

    @Test
    void allModuleConfigurationPropertiesBeansExist() {
        // Verify that all module configuration properties beans are created
        assertTrue(applicationContext.containsBean("ingestionProperties"), 
            "IngestionProperties bean should exist");
        assertTrue(applicationContext.containsBean("dataQualityProperties"), 
            "DataQualityProperties bean should exist");
        assertTrue(applicationContext.containsBean("riskCalculationProperties"), 
            "RiskCalculationProperties bean should exist");
        assertTrue(applicationContext.containsBean("reportGenerationProperties"), 
            "ReportGenerationProperties bean should exist");
        assertTrue(applicationContext.containsBean("billingProperties"), 
            "BillingProperties bean should exist");
        assertTrue(applicationContext.containsBean("iamProperties"), 
            "IAMProperties bean should exist");
    }

    @Test
    void sharedInfrastructureBeansAreLoaded() {
        // Verify that shared infrastructure beans are loaded
        assertTrue(applicationContext.containsBean("dataSource"), 
            "DataSource bean should be loaded");
        assertTrue(applicationContext.containsBean("entityManagerFactory"), 
            "EntityManagerFactory bean should be loaded");
    }

    @Test
    void asyncConfigurationBeansAreLoaded() {
        // Verify that async configuration beans from each module are loaded
        assertTrue(applicationContext.containsBean("ingestionTaskExecutor") || 
                   applicationContext.containsBean("ingestionAsyncConfiguration"), 
            "Ingestion async configuration should be loaded");
        assertTrue(applicationContext.containsBean("dataQualityTaskExecutor") || 
                   applicationContext.containsBean("dataQualityAsyncConfiguration"), 
            "Data quality async configuration should be loaded");
        assertTrue(applicationContext.containsBean("riskCalculationTaskExecutor") || 
                   applicationContext.containsBean("riskCalculationAsyncConfiguration"), 
            "Risk calculation async configuration should be loaded");
        assertTrue(applicationContext.containsBean("reportGenerationTaskExecutor") || 
                   applicationContext.containsBean("reportGenerationAsyncConfiguration"), 
            "Report generation async configuration should be loaded");
    }

    @Test
    void securityConfigurationIsLoaded() {
        // Verify that security configuration is loaded
        assertTrue(applicationContext.containsBean("securityFilter"), 
            "SecurityFilter bean should be loaded");
    }
}
