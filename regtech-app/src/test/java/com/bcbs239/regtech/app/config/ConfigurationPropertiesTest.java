package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.billing.infrastructure.config.BillingProperties;
import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import com.bcbs239.regtech.modules.dataquality.infrastructure.config.DataQualityProperties;
import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionProperties;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that all @ConfigurationProperties classes bind correctly and validation works.
 * Requirements: 6.5, 10.1, 10.2
 */
@SpringBootTest
@ActiveProfiles("development")
class ConfigurationPropertiesTest {

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
    void ingestionPropertiesBindCorrectly() {
        assertNotNull(ingestionProperties, "IngestionProperties should be bound");
        assertNotNull(ingestionProperties.getEnabled(), "Enabled flag should be set");
        assertNotNull(ingestionProperties.getFile(), "File properties should be bound");
        assertNotNull(ingestionProperties.getStorage(), "Storage properties should be bound");
        assertNotNull(ingestionProperties.getAsync(), "Async properties should be bound");
        assertNotNull(ingestionProperties.getPerformance(), "Performance properties should be bound");
    }

    @Test
    void dataQualityPropertiesBindCorrectly() {
        assertNotNull(dataQualityProperties, "DataQualityProperties should be bound");
        assertNotNull(dataQualityProperties.getEnabled(), "Enabled flag should be set");
        assertNotNull(dataQualityProperties.getStorage(), "Storage properties should be bound");
        assertNotNull(dataQualityProperties.getAsync(), "Async properties should be bound");
        assertNotNull(dataQualityProperties.getRulesEngine(), "Rules engine properties should be bound");
    }

    @Test
    void riskCalculationPropertiesBindCorrectly() {
        assertNotNull(riskCalculationProperties, "RiskCalculationProperties should be bound");
        assertNotNull(riskCalculationProperties.getEnabled(), "Enabled flag should be set");
        assertNotNull(riskCalculationProperties.getStorage(), "Storage properties should be bound");
        assertNotNull(riskCalculationProperties.getAsync(), "Async properties should be bound");
        assertNotNull(riskCalculationProperties.getCurrency(), "Currency properties should be bound");
        assertNotNull(riskCalculationProperties.getGeographic(), "Geographic properties should be bound");
    }

    @Test
    void reportGenerationPropertiesBindCorrectly() {
        assertNotNull(reportGenerationProperties, "ReportGenerationProperties should be bound");
        assertNotNull(reportGenerationProperties.getEnabled(), "Enabled flag should be set");
        assertNotNull(reportGenerationProperties.getS3(), "S3 properties should be bound");
        assertNotNull(reportGenerationProperties.getAsync(), "Async properties should be bound");
        assertNotNull(reportGenerationProperties.getCoordination(), "Coordination properties should be bound");
    }

    @Test
    void billingPropertiesBindCorrectly() {
        assertNotNull(billingProperties, "BillingProperties should be bound");
        assertNotNull(billingProperties.getEnabled(), "Enabled flag should be set");
        assertNotNull(billingProperties.getStripe(), "Stripe properties should be bound");
        assertNotNull(billingProperties.getTiers(), "Tiers properties should be bound");
        assertNotNull(billingProperties.getDunning(), "Dunning properties should be bound");
    }

    @Test
    void iamPropertiesBindCorrectly() {
        assertNotNull(iamProperties, "IAMProperties should be bound");
        assertNotNull(iamProperties.getEnabled(), "Enabled flag should be set");
        assertNotNull(iamProperties.getSecurity(), "Security properties should be bound");
        assertNotNull(iamProperties.getAuthorization(), "Authorization properties should be bound");
        assertNotNull(iamProperties.getUserManagement(), "User management properties should be bound");
    }

    @Test
    void defaultValuesAreSetWhenPropertiesNotSpecified() {
        // Test that default values are used when properties are not explicitly set
        // Ingestion defaults
        assertTrue(ingestionProperties.getEnabled(), "Ingestion should be enabled by default");
        assertEquals(524288000L, ingestionProperties.getFile().getMaxSize(), 
            "Default max file size should be 500MB");
        
        // IAM defaults
        assertTrue(iamProperties.getEnabled(), "IAM should be enabled by default");
        assertEquals(86400, iamProperties.getSecurity().getJwt().getExpiration(), 
            "Default JWT expiration should be 24 hours");
        assertEquals(12, iamProperties.getSecurity().getPassword().getMinLength(), 
            "Default password min length should be 12");
    }

    @Test
    void validationAnnotationsAreRespected() {
        // Test that validation annotations work correctly
        // Thread pool sizes should be positive
        assertTrue(ingestionProperties.getAsync().getCorePoolSize() > 0, 
            "Core pool size should be positive");
        assertTrue(ingestionProperties.getAsync().getMaxPoolSize() > 0, 
            "Max pool size should be positive");
        
        // File size should be positive
        assertTrue(ingestionProperties.getFile().getMaxSize() > 0, 
            "Max file size should be positive");
        
        // JWT expiration should be positive
        assertTrue(iamProperties.getSecurity().getJwt().getExpiration() > 0, 
            "JWT expiration should be positive");
        
        // Password min length should be at least 8
        assertTrue(iamProperties.getSecurity().getPassword().getMinLength() >= 8, 
            "Password min length should be at least 8");
    }

    @Test
    void nestedPropertiesBindCorrectly() {
        // Test that nested properties are bound correctly
        assertNotNull(ingestionProperties.getStorage().getS3(), 
            "Nested S3 properties should be bound");
        assertNotNull(ingestionProperties.getStorage().getLocal(), 
            "Nested local storage properties should be bound");
        
        assertNotNull(iamProperties.getSecurity().getJwt(), 
            "Nested JWT properties should be bound");
        assertNotNull(iamProperties.getSecurity().getPassword(), 
            "Nested password properties should be bound");
        assertNotNull(iamProperties.getSecurity().getOauth2(), 
            "Nested OAuth2 properties should be bound");
    }

    @Test
    void storageTypeConfigurationIsValid() {
        // Test that storage type is valid
        String storageType = ingestionProperties.getStorage().getType();
        assertNotNull(storageType, "Storage type should be set");
        assertTrue(storageType.equals("s3") || storageType.equals("local"), 
            "Storage type should be either 's3' or 'local'");
    }

    @Test
    void threadPoolConfigurationIsValid() {
        // Test that thread pool configuration is valid
        // Core pool size should be <= max pool size
        assertTrue(ingestionProperties.getAsync().getCorePoolSize() <= 
                   ingestionProperties.getAsync().getMaxPoolSize(), 
            "Core pool size should be <= max pool size for ingestion");
        
        assertTrue(dataQualityProperties.getAsync().getCorePoolSize() <= 
                   dataQualityProperties.getAsync().getMaxPoolSize(), 
            "Core pool size should be <= max pool size for data quality");
        
        assertTrue(riskCalculationProperties.getAsync().getCorePoolSize() <= 
                   riskCalculationProperties.getAsync().getMaxPoolSize(), 
            "Core pool size should be <= max pool size for risk calculation");
        
        assertTrue(reportGenerationProperties.getAsync().getCorePoolSize() <= 
                   reportGenerationProperties.getAsync().getMaxPoolSize(), 
            "Core pool size should be <= max pool size for report generation");
    }

    @Test
    void publicPathsConfigurationIsValid() {
        // Test that public paths are configured
        assertNotNull(iamProperties.getSecurity().getPublicPaths(), 
            "Public paths should be configured");
        assertFalse(iamProperties.getSecurity().getPublicPaths().isEmpty(), 
            "Public paths should not be empty");
        
        // Verify common public paths are present
        assertTrue(iamProperties.getSecurity().getPublicPaths().stream()
            .anyMatch(path -> path.contains("/health")), 
            "Health endpoints should be in public paths");
    }
}
