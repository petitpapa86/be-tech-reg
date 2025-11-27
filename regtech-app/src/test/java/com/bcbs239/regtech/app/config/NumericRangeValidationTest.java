package com.bcbs239.regtech.app.config;

import com.bcbs239.regtech.iam.infrastructure.config.IAMProperties;
import com.bcbs239.regtech.dataquality.infrastructure.config.DataQualityProperties;
import com.bcbs239.regtech.modules.ingestion.infrastructure.config.IngestionProperties;
import com.bcbs239.regtech.reportgeneration.infrastructure.config.ReportGenerationProperties;
import com.bcbs239.regtech.riskcalculation.infrastructure.config.RiskCalculationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that numeric range validation works correctly for configuration properties.
 * Requirements: 10.3, 13.5
 */
@SpringBootTest
@ActiveProfiles("development")
class NumericRangeValidationTest {

    @Autowired
    private IngestionProperties ingestionProperties;

    @Autowired
    private DataQualityProperties dataQualityProperties;

    @Autowired
    private RiskCalculationProperties riskCalculationProperties;

    @Autowired
    private ReportGenerationProperties reportGenerationProperties;

    @Autowired
    private IAMProperties iamProperties;

    @Test
    void threadPoolSizesMustBePositive() {
        // All thread pool sizes must be greater than 0
        assertTrue(ingestionProperties.getAsync().getCorePoolSize() > 0,
            "Ingestion core pool size must be positive");
        assertTrue(ingestionProperties.getAsync().getMaxPoolSize() > 0,
            "Ingestion max pool size must be positive");

        assertTrue(dataQualityProperties.getAsync().getCorePoolSize() > 0,
            "Data quality core pool size must be positive");
        assertTrue(dataQualityProperties.getAsync().getMaxPoolSize() > 0,
            "Data quality max pool size must be positive");

        assertTrue(riskCalculationProperties.getAsync().getCorePoolSize() > 0,
            "Risk calculation core pool size must be positive");
        assertTrue(riskCalculationProperties.getAsync().getMaxPoolSize() > 0,
            "Risk calculation max pool size must be positive");

        assertTrue(reportGenerationProperties.getAsync().getCorePoolSize() > 0,
            "Report generation core pool size must be positive");
        assertTrue(reportGenerationProperties.getAsync().getMaxPoolSize() > 0,
            "Report generation max pool size must be positive");
    }

    @Test
    void corePoolSizeMustBeLessThanOrEqualToMaxPoolSize() {
        // Core pool size must be <= max pool size
        assertTrue(ingestionProperties.getAsync().getCorePoolSize() <=
                   ingestionProperties.getAsync().getMaxPoolSize(),
            "Ingestion core pool size must be <= max pool size");

        assertTrue(dataQualityProperties.getAsync().getCorePoolSize() <=
                   dataQualityProperties.getAsync().getMaxPoolSize(),
            "Data quality core pool size must be <= max pool size");

        assertTrue(riskCalculationProperties.getAsync().getCorePoolSize() <=
                   riskCalculationProperties.getAsync().getMaxPoolSize(),
            "Risk calculation core pool size must be <= max pool size");

        assertTrue(reportGenerationProperties.getAsync().getCorePoolSize() <=
                   reportGenerationProperties.getAsync().getMaxPoolSize(),
            "Report generation core pool size must be <= max pool size");
    }

    @Test
    void queueCapacityMustBeNonNegative() {
        // Queue capacity must be >= 0
        assertTrue(ingestionProperties.getAsync().getQueueCapacity() >= 0,
            "Ingestion queue capacity must be non-negative");

        assertTrue(dataQualityProperties.getAsync().getQueueCapacity() >= 0,
            "Data quality queue capacity must be non-negative");

        assertTrue(riskCalculationProperties.getAsync().getQueueCapacity() >= 0,
            "Risk calculation queue capacity must be non-negative");

        assertTrue(reportGenerationProperties.getAsync().getQueueCapacity() >= 0,
            "Report generation queue capacity must be non-negative");
    }

    @Test
    void timeoutValuesMustBePositive() {
        // Timeout values must be positive
        assertTrue(ingestionProperties.getAsync().getAwaitTerminationSeconds() >= 0,
            "Ingestion await termination seconds must be non-negative");

        assertTrue(dataQualityProperties.getAsync().getAwaitTerminationSeconds() >= 0,
            "Data quality await termination seconds must be non-negative");

        assertTrue(riskCalculationProperties.getAsync().getAwaitTerminationSeconds() >= 0,
            "Risk calculation await termination seconds must be non-negative");

        assertTrue(reportGenerationProperties.getAsync().getAwaitTerminationSeconds() >= 0,
            "Report generation await termination seconds must be non-negative");
    }

    @Test
    void fileSizeLimitsMustBePositive() {
        // File size limits must be positive
        assertTrue(ingestionProperties.getFile().getMaxSize() > 0,
            "Ingestion max file size must be positive");
    }

    @Test
    void jwtExpirationMustBePositive() {
        // JWT expiration must be positive
        assertTrue(iamProperties.getSecurity().getJwt().getExpiration() > 0,
            "JWT expiration must be positive");
    }

    @Test
    void passwordMinLengthMustBeAtLeast8() {
        // Password min length must be at least 8
        assertTrue(iamProperties.getSecurity().getPassword().getMinLength() >= 8,
            "Password min length must be at least 8");
    }

    @Test
    void cacheTtlMustBeNonNegative() {
        // Cache TTL must be non-negative
        assertTrue(iamProperties.getAuthorization().getCache().getTtl() >= 0,
            "Cache TTL must be non-negative");
    }

    @Test
    void sessionTimeoutMustBePositive() {
        // Session timeout must be positive
        assertTrue(iamProperties.getSession().getTimeout() > 0,
            "Session timeout must be positive");
        assertTrue(iamProperties.getSession().getMaxConcurrentSessions() > 0,
            "Max concurrent sessions must be positive");
    }

    @Test
    void userManagementTimeoutsMustBePositive() {
        // User management timeouts must be positive
        assertTrue(iamProperties.getUserManagement().getPasswordResetTokenExpiration() > 0,
            "Password reset token expiration must be positive");
        assertTrue(iamProperties.getUserManagement().getEmailVerificationTokenExpiration() > 0,
            "Email verification token expiration must be positive");
    }

    @Test
    void lockoutConfigurationMustBeValid() {
        // Lockout configuration must be valid
        assertTrue(iamProperties.getUserManagement().getLockout().getMaxFailedAttempts() > 0,
            "Max failed attempts must be positive");
        assertTrue(iamProperties.getUserManagement().getLockout().getLockoutDuration() > 0,
            "Lockout duration must be positive");
    }

    @Test
    void performanceSettingsMustBePositive() {
        // Performance settings must be positive
        assertTrue(ingestionProperties.getPerformance().getMaxConcurrentFiles() > 0,
            "Max concurrent files must be positive");
        assertTrue(ingestionProperties.getPerformance().getChunkSize() > 0,
            "Chunk size must be positive");
    }

    @Test
    void performanceTargetsMustBePositive() {
        // Performance targets must be positive
        assertTrue(reportGenerationProperties.getPerformance().getDataFetchTarget() > 0,
            "Data fetch target must be positive");
        assertTrue(reportGenerationProperties.getPerformance().getHtmlGenerationTarget() > 0,
            "HTML generation target must be positive");
        assertTrue(reportGenerationProperties.getPerformance().getXbrlGenerationTarget() > 0,
            "XBRL generation target must be positive");
        assertTrue(reportGenerationProperties.getPerformance().getTotalGenerationTarget() > 0,
            "Total generation target must be positive");
    }

    @Test
    void retryConfigurationMustBeValid() {
        // Retry configuration must be valid
        assertTrue(reportGenerationProperties.getRetry().getMaxRetries() >= 0,
            "Retry max retries must be non-negative");
        assertNotNull(reportGenerationProperties.getRetry().getBackoffIntervalsSeconds(),
            "Retry backoff intervals must be specified");
        assertFalse(reportGenerationProperties.getRetry().getBackoffIntervalsSeconds().isEmpty(),
            "Retry backoff intervals must not be empty");
    }

    @Test
    void coordinationTimeoutsMustBePositive() {
        // Coordination timeouts must be positive (updated to match actual properties)
        assertTrue(reportGenerationProperties.getCoordination().getEventExpirationHours() > 0,
            "Event expiration hours must be positive");
        assertTrue(reportGenerationProperties.getCoordination().getCleanupIntervalMinutes() > 0,
            "Cleanup interval minutes must be positive");
    }
}
