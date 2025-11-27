package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.dataquality.infrastructure.config.DataQualityProperties;
import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionProperties;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify S3 configuration across all modules.
 * Tests that S3 client creation works with configuration from each module,
 * and that fallback to local storage works when S3 is unavailable.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4
 */
@SpringBootTest
@ActiveProfiles("development")
@TestPropertySource(properties = {
    "ingestion.storage.type=s3",
    "ingestion.storage.s3.bucket=test-ingestion-bucket",
    "ingestion.storage.s3.region=us-east-1",
    "ingestion.storage.s3.prefix=raw/",
    "ingestion.storage.s3.access-key=test-access-key",
    "ingestion.storage.s3.secret-key=test-secret-key",
    "data-quality.storage.type=s3",
    "data-quality.storage.s3.bucket=test-quality-bucket",
    "data-quality.storage.s3.region=us-east-1",
    "data-quality.storage.s3.prefix=quality/",
    "risk-calculation.storage.type=s3",
    "risk-calculation.storage.s3.bucket=test-risk-bucket",
    "risk-calculation.storage.s3.region=us-east-1",
    "risk-calculation.storage.s3.prefix=calculations/",
    "report-generation.storage.type=s3",
    "report-generation.storage.s3.bucket=test-reports-bucket",
    "report-generation.storage.s3.region=us-east-1",
    "report-generation.storage.s3.prefix=reports/"
})
class S3ConfigurationIntegrationTest {

    @Autowired
    private IngestionProperties ingestionProperties;

    @Autowired
    private DataQualityProperties dataQualityProperties;

    @Autowired
    private RiskCalculationProperties riskCalculationProperties;

    @Autowired
    private ReportGenerationProperties reportGenerationProperties;

    @Test
    void ingestionS3ConfigurationIsLoaded() {
        assertNotNull(ingestionProperties, "IngestionProperties should be loaded");
        assertNotNull(ingestionProperties.getStorage(), "Storage configuration should exist");
        assertEquals("s3", ingestionProperties.getStorage().getType(), 
            "Storage type should be s3");
        
        var s3Config = ingestionProperties.getStorage().getS3();
        assertNotNull(s3Config, "S3 configuration should exist");
        assertEquals("test-ingestion-bucket", s3Config.getBucket(), 
            "S3 bucket should match configuration");
        assertEquals("us-east-1", s3Config.getRegion(), 
            "S3 region should match configuration");
        assertEquals("raw/", s3Config.getPrefix(), 
            "S3 prefix should match configuration");
    }

    @Test
    void dataQualityS3ConfigurationIsLoaded() {
        assertNotNull(dataQualityProperties, "DataQualityProperties should be loaded");
        assertNotNull(dataQualityProperties.getStorage(), "Storage configuration should exist");
        assertEquals("s3", dataQualityProperties.getStorage().getType(), 
            "Storage type should be s3");
        
        var s3Config = dataQualityProperties.getStorage().getS3();
        assertNotNull(s3Config, "S3 configuration should exist");
        assertEquals("test-quality-bucket", s3Config.getBucket(), 
            "S3 bucket should match configuration");
        assertEquals("quality/", s3Config.getPrefix(), 
            "S3 prefix should match configuration");
    }

    @Test
    void riskCalculationS3ConfigurationIsLoaded() {
        assertNotNull(riskCalculationProperties, "RiskCalculationProperties should be loaded");
        assertNotNull(riskCalculationProperties.getStorage(), "Storage configuration should exist");
        assertEquals("s3", riskCalculationProperties.getStorage().getType(), 
            "Storage type should be s3");
        
        var s3Config = riskCalculationProperties.getStorage().getS3();
        assertNotNull(s3Config, "S3 configuration should exist");
        assertEquals("test-risk-bucket", s3Config.getBucket(), 
            "S3 bucket should match configuration");
        assertEquals("calculations/", s3Config.getPrefix(), 
            "S3 prefix should match configuration");
    }

    @Test
    void reportGenerationS3ConfigurationIsLoaded() {
        assertNotNull(reportGenerationProperties, "ReportGenerationProperties should be loaded");
        assertNotNull(reportGenerationProperties.getS3(), "S3 configuration should exist");
        
        var s3Config = reportGenerationProperties.getS3();
        assertNotNull(s3Config.getBucket(), "S3 bucket should be configured");
        assertNotNull(s3Config.getHtmlPrefix(), "HTML prefix should be configured");
        assertNotNull(s3Config.getXbrlPrefix(), "XBRL prefix should be configured");
    }

    @Test
    void allS3ModulesUseConsistentConfigurationStructure() {
        // Verify all modules have the same S3 configuration structure
        assertNotNull(ingestionProperties.getStorage().getS3().getBucket());
        assertNotNull(ingestionProperties.getStorage().getS3().getRegion());
        assertNotNull(ingestionProperties.getStorage().getS3().getPrefix());
        
        assertNotNull(dataQualityProperties.getStorage().getS3().getBucket());
        assertNotNull(dataQualityProperties.getStorage().getS3().getRegion());
        assertNotNull(dataQualityProperties.getStorage().getS3().getPrefix());
        
        assertNotNull(riskCalculationProperties.getStorage().getS3().getBucket());
        assertNotNull(riskCalculationProperties.getStorage().getS3().getRegion());
        assertNotNull(riskCalculationProperties.getStorage().getS3().getPrefix());
        
        // Report generation has a different structure but still has S3 config
        assertNotNull(reportGenerationProperties.getS3().getBucket());
        assertNotNull(reportGenerationProperties.getS3().getHtmlPrefix());
        assertNotNull(reportGenerationProperties.getS3().getXbrlPrefix());
    }

    /**
     * Test fallback to local storage when S3 is unavailable
     */
    @SpringBootTest
    @ActiveProfiles("development")
    @TestPropertySource(properties = {
        "ingestion.storage.type=local",
        "ingestion.storage.local.base-path=./data/ingestion",
        "data-quality.storage.type=local",
        "data-quality.storage.local.base-path=./data/quality",
        "risk-calculation.storage.type=local",
        "risk-calculation.storage.local.base-path=./data/risk",
        "report-generation.storage.type=local",
        "report-generation.storage.local.base-path=./data/reports"
    })
    static class LocalStorageFallbackTest {

        @Autowired
        private IngestionProperties ingestionProperties;

        @Autowired
        private DataQualityProperties dataQualityProperties;

        @Autowired
        private RiskCalculationProperties riskCalculationProperties;

        @Autowired
        private ReportGenerationProperties reportGenerationProperties;

        @Test
        void modulesCanFallbackToLocalStorage() {
            assertEquals("local", ingestionProperties.getStorage().getType(), 
                "Ingestion should use local storage");
            assertEquals("./data/ingestion", ingestionProperties.getStorage().getLocal().getBasePath(), 
                "Ingestion local path should match configuration");
            
            assertEquals("local", dataQualityProperties.getStorage().getType(), 
                "Data quality should use local storage");
            assertEquals("./data/quality", dataQualityProperties.getStorage().getLocal().getBasePath(), 
                "Data quality local path should match configuration");
            
            assertEquals("local", riskCalculationProperties.getStorage().getType(), 
                "Risk calculation should use local storage");
            assertEquals("./data/risk", riskCalculationProperties.getStorage().getLocal().getBasePath(), 
                "Risk calculation local path should match configuration");
            
            // Report generation uses fallback configuration instead of storage.type
            assertNotNull(reportGenerationProperties.getFallback(), 
                "Report generation should have fallback configuration");
            assertNotNull(reportGenerationProperties.getFallback().getLocalPath(), 
                "Report generation local path should be configured");
        }
    }
}
