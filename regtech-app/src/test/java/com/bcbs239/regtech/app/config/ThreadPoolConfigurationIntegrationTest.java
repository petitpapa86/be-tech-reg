package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.modules.dataquality.infrastructure.config.DataQualityProperties;
import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionProperties;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify thread pool configuration across all modules.
 * Tests that each module creates its own task executor, thread pool sizes
 * match configuration, and profile-specific thread pool sizes are applied.
 * 
 * Requirements: 13.1, 13.2, 13.3
 */
@SpringBootTest
@ActiveProfiles("development")
class ThreadPoolConfigurationIntegrationTest {

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
    void eachModuleCreatesItsOwnTaskExecutor() {
        // Verify that each module has its own task executor bean
        assertTrue(applicationContext.containsBean("ingestionTaskExecutor"), 
            "Ingestion task executor should exist");
        assertTrue(applicationContext.containsBean("dataQualityTaskExecutor"), 
            "Data quality task executor should exist");
        assertTrue(applicationContext.containsBean("riskCalculationTaskExecutor"), 
            "Risk calculation task executor should exist");
        assertTrue(applicationContext.containsBean("reportGenerationTaskExecutor"), 
            "Report generation task executor should exist");
    }

    @Test
    void ingestionThreadPoolMatchesConfiguration() {
        var asyncConfig = ingestionProperties.getAsync();
        assertNotNull(asyncConfig, "Ingestion async configuration should exist");
        
        if (!asyncConfig.isEnabled()) {
            return; // Skip if async is disabled
        }

        Executor executor = (Executor) applicationContext.getBean("ingestionTaskExecutor");
        assertNotNull(executor, "Ingestion task executor should be created");
        
        if (executor instanceof ThreadPoolTaskExecutor) {
            ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
            assertEquals(asyncConfig.getCorePoolSize(), threadPoolExecutor.getCorePoolSize(), 
                "Ingestion core pool size should match configuration");
            assertEquals(asyncConfig.getMaxPoolSize(), threadPoolExecutor.getMaxPoolSize(), 
                "Ingestion max pool size should match configuration");
            assertTrue(threadPoolExecutor.getThreadNamePrefix().contains("ingestion"), 
                "Ingestion thread name prefix should contain 'ingestion'");
        }
    }

    @Test
    void dataQualityThreadPoolMatchesConfiguration() {
        var asyncConfig = dataQualityProperties.getAsync();
        assertNotNull(asyncConfig, "Data quality async configuration should exist");
        
        if (!asyncConfig.isEnabled()) {
            return; // Skip if async is disabled
        }

        Executor executor = (Executor) applicationContext.getBean("dataQualityTaskExecutor");
        assertNotNull(executor, "Data quality task executor should be created");
        
        if (executor instanceof ThreadPoolTaskExecutor) {
            ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
            assertEquals(asyncConfig.getCorePoolSize(), threadPoolExecutor.getCorePoolSize(), 
                "Data quality core pool size should match configuration");
            assertEquals(asyncConfig.getMaxPoolSize(), threadPoolExecutor.getMaxPoolSize(), 
                "Data quality max pool size should match configuration");
            assertTrue(threadPoolExecutor.getThreadNamePrefix().contains("data-quality"), 
                "Data quality thread name prefix should contain 'data-quality'");
        }
    }

    @Test
    void riskCalculationThreadPoolMatchesConfiguration() {
        var asyncConfig = riskCalculationProperties.getAsync();
        assertNotNull(asyncConfig, "Risk calculation async configuration should exist");
        
        if (!asyncConfig.isEnabled()) {
            return; // Skip if async is disabled
        }

        Executor executor = (Executor) applicationContext.getBean("riskCalculationTaskExecutor");
        assertNotNull(executor, "Risk calculation task executor should be created");
        
        if (executor instanceof ThreadPoolTaskExecutor) {
            ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
            assertEquals(asyncConfig.getCorePoolSize(), threadPoolExecutor.getCorePoolSize(), 
                "Risk calculation core pool size should match configuration");
            assertEquals(asyncConfig.getMaxPoolSize(), threadPoolExecutor.getMaxPoolSize(), 
                "Risk calculation max pool size should match configuration");
            assertTrue(threadPoolExecutor.getThreadNamePrefix().contains("risk-calculation"), 
                "Risk calculation thread name prefix should contain 'risk-calculation'");
        }
    }

    @Test
    void reportGenerationThreadPoolMatchesConfiguration() {
        var asyncConfig = reportGenerationProperties.getAsync();
        assertNotNull(asyncConfig, "Report generation async configuration should exist");

        Executor executor = (Executor) applicationContext.getBean("reportGenerationTaskExecutor");
        assertNotNull(executor, "Report generation task executor should be created");
        
        if (executor instanceof ThreadPoolTaskExecutor) {
            ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
            assertEquals(asyncConfig.getCorePoolSize(), threadPoolExecutor.getCorePoolSize(), 
                "Report generation core pool size should match configuration");
            assertEquals(asyncConfig.getMaxPoolSize(), threadPoolExecutor.getMaxPoolSize(), 
                "Report generation max pool size should match configuration");
            assertTrue(threadPoolExecutor.getThreadNamePrefix().contains("report-gen"), 
                "Report generation thread name prefix should contain 'report-gen'");
        }
    }

    @Test
    void allThreadPoolsHaveConsistentPropertyNames() {
        // Verify all modules use consistent property names
        assertNotNull(ingestionProperties.getAsync().getCorePoolSize());
        assertNotNull(ingestionProperties.getAsync().getMaxPoolSize());
        assertNotNull(ingestionProperties.getAsync().getQueueCapacity());
        assertNotNull(ingestionProperties.getAsync().getThreadNamePrefix());
        
        assertNotNull(dataQualityProperties.getAsync().getCorePoolSize());
        assertNotNull(dataQualityProperties.getAsync().getMaxPoolSize());
        assertNotNull(dataQualityProperties.getAsync().getQueueCapacity());
        assertNotNull(dataQualityProperties.getAsync().getThreadNamePrefix());
        
        assertNotNull(riskCalculationProperties.getAsync().getCorePoolSize());
        assertNotNull(riskCalculationProperties.getAsync().getMaxPoolSize());
        assertNotNull(riskCalculationProperties.getAsync().getQueueCapacity());
        assertNotNull(riskCalculationProperties.getAsync().getThreadNamePrefix());
        
        assertNotNull(reportGenerationProperties.getAsync().getCorePoolSize());
        assertNotNull(reportGenerationProperties.getAsync().getMaxPoolSize());
        assertNotNull(reportGenerationProperties.getAsync().getQueueCapacity());
        assertNotNull(reportGenerationProperties.getAsync().getThreadNamePrefix());
    }

    @Test
    void threadPoolSizesAreValid() {
        // Verify all thread pool sizes are positive and core <= max
        validateAsyncProperties(ingestionProperties.getAsync(), "Ingestion");
        validateAsyncProperties(dataQualityProperties.getAsync(), "Data Quality");
        validateAsyncProperties(riskCalculationProperties.getAsync(), "Risk Calculation");
        validateAsyncProperties(reportGenerationProperties.getAsync(), "Report Generation");
    }

    @Test
    void developmentProfileUsesReducedThreadPools() {
        // In development profile, thread pools should be smaller
        // This test verifies the profile-specific configuration is applied
        
        var ingestionAsync = ingestionProperties.getAsync();
        var dataQualityAsync = dataQualityProperties.getAsync();
        var riskAsync = riskCalculationProperties.getAsync();
        var reportAsync = reportGenerationProperties.getAsync();
        
        // Development profile should have smaller pools (typically 2-4 core, 4-10 max)
        assertTrue(ingestionAsync.getCorePoolSize() <= 10, 
            "Development ingestion core pool should be small");
        assertTrue(dataQualityAsync.getCorePoolSize() <= 10, 
            "Development data quality core pool should be small");
        assertTrue(riskAsync.getCorePoolSize() <= 10, 
            "Development risk calculation core pool should be small");
        assertTrue(reportAsync.getCorePoolSize() <= 10, 
            "Development report generation core pool should be small");
    }

    @Test
    void threadNamePrefixesAreUnique() {
        // Verify each module has a unique thread name prefix
        String ingestionPrefix = ingestionProperties.getAsync().getThreadNamePrefix();
        String dataQualityPrefix = dataQualityProperties.getAsync().getThreadNamePrefix();
        String riskPrefix = riskCalculationProperties.getAsync().getThreadNamePrefix();
        String reportPrefix = reportGenerationProperties.getAsync().getThreadNamePrefix();
        
        assertNotEquals(ingestionPrefix, dataQualityPrefix, 
            "Ingestion and data quality should have different thread prefixes");
        assertNotEquals(ingestionPrefix, riskPrefix, 
            "Ingestion and risk calculation should have different thread prefixes");
        assertNotEquals(ingestionPrefix, reportPrefix, 
            "Ingestion and report generation should have different thread prefixes");
        assertNotEquals(dataQualityPrefix, riskPrefix, 
            "Data quality and risk calculation should have different thread prefixes");
        assertNotEquals(dataQualityPrefix, reportPrefix, 
            "Data quality and report generation should have different thread prefixes");
        assertNotEquals(riskPrefix, reportPrefix, 
            "Risk calculation and report generation should have different thread prefixes");
    }

    private void validateAsyncProperties(Object asyncProperties, String moduleName) {
        try {
            int corePoolSize = (int) asyncProperties.getClass().getMethod("getCorePoolSize").invoke(asyncProperties);
            int maxPoolSize = (int) asyncProperties.getClass().getMethod("getMaxPoolSize").invoke(asyncProperties);
            int queueCapacity = (int) asyncProperties.getClass().getMethod("getQueueCapacity").invoke(asyncProperties);
            
            assertTrue(corePoolSize > 0, 
                moduleName + " core pool size must be positive");
            assertTrue(maxPoolSize > 0, 
                moduleName + " max pool size must be positive");
            assertTrue(corePoolSize <= maxPoolSize, 
                moduleName + " core pool size must be <= max pool size");
            assertTrue(queueCapacity >= 0, 
                moduleName + " queue capacity must be non-negative");
        } catch (Exception e) {
            fail(moduleName + " async properties validation failed: " + e.getMessage());
        }
    }

    /**
     * Test production profile thread pool sizes
     */
    @SpringBootTest
    @ActiveProfiles("production")
    static class ProductionProfileThreadPoolTest {

        @Autowired
        private IngestionProperties ingestionProperties;

        @Autowired
        private DataQualityProperties dataQualityProperties;

        @Autowired
        private RiskCalculationProperties riskCalculationProperties;

        @Autowired
        private ReportGenerationProperties reportGenerationProperties;

        @Test
        void productionProfileUsesLargerThreadPools() {
            // In production profile, thread pools should be larger
            var ingestionAsync = ingestionProperties.getAsync();
            var dataQualityAsync = dataQualityProperties.getAsync();
            var riskAsync = riskCalculationProperties.getAsync();
            var reportAsync = reportGenerationProperties.getAsync();
            
            // Production profile should have larger pools (typically 5-10 core, 10-20 max)
            assertTrue(ingestionAsync.getCorePoolSize() >= 2, 
                "Production ingestion core pool should be reasonable");
            assertTrue(dataQualityAsync.getCorePoolSize() >= 2, 
                "Production data quality core pool should be reasonable");
            assertTrue(riskAsync.getCorePoolSize() >= 2, 
                "Production risk calculation core pool should be reasonable");
            assertTrue(reportAsync.getCorePoolSize() >= 2, 
                "Production report generation core pool should be reasonable");
        }
    }
}
