package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionProperties;
import com.bcbs239.regtech.modules.dataquality.infrastructure.config.DataQualityProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the application starts successfully
 * with the production profile.
 * 
 * Tests that:
 * - Application starts with production profile
 * - Production-specific configuration is applied
 * - S3 storage is configured for production
 * 
 * Requirements: 7.1, 7.3
 */
@SpringBootTest
@ActiveProfiles("production")
class ProductionProfileStartupTest {

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

    @Test
    void applicationStartsWithProductionProfile() {
        assertNotNull(applicationContext, "Application context should load with production profile");
        
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        assertTrue(activeProfiles.length > 0, "At least one profile should be active");
        assertEquals("production", activeProfiles[0], "Production profile should be active");
    }

    @Test
    void productionProfileUsesS3Storage() {
        // Verify that production profile uses S3 storage
        assertEquals("s3", ingestionProperties.getStorage().getType(),
            "Production profile should use S3 storage for ingestion");
        assertEquals("s3", dataQualityProperties.getStorage().getType(),
            "Production profile should use S3 storage for data quality");
        assertEquals("s3", riskCalculationProperties.getStorage().getType(),
            "Production profile should use S3 storage for risk calculation");
    }

    @Test
    void productionProfileHasOptimizedThreadPools() {
        // Verify that production profile has larger thread pools
        assertTrue(ingestionProperties.getAsync().getCorePoolSize() >= 5,
            "Production should have larger core pool size for ingestion");
        assertTrue(dataQualityProperties.getAsync().getCorePoolSize() >= 5,
            "Production should have larger core pool size for data quality");
        assertTrue(riskCalculationProperties.getAsync().getCorePoolSize() >= 5,
            "Production should have larger core pool size for risk calculation");
    }

    @Test
    void allModulesLoadSuccessfully() {
        // Verify all modules are enabled and configured
        assertTrue(ingestionProperties.getEnabled(), "Ingestion module should be enabled");
        assertTrue(dataQualityProperties.getEnabled(), "Data quality module should be enabled");
        assertTrue(riskCalculationProperties.getEnabled(), "Risk calculation module should be enabled");
        assertTrue(reportGenerationProperties.getEnabled(), "Report generation module should be enabled");
    }
}
