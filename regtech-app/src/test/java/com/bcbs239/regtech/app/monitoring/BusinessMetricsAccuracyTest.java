package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusinessMetricsCollector accuracy.
 * 
 * Feature: observability-enhancement
 * Property 9: Business metrics accuracy
 * Validates: Requirements 2.5
 * 
 * For any business operation completion, the system should record domain-specific 
 * metrics with correct values and appropriate business context.
 */
class BusinessMetricsAccuracyTest {

    private MeterRegistry meterRegistry;
    private BusinessMetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsCollector = new BusinessMetricsCollector(meterRegistry);
    }

    @Test
    void dataQualityScoreMetricsAreAccurate() {
        // Given
        String batchId = "test-batch-123";
        double qualityScore = 0.85;
        
        // When: Recording a data quality score
        metricsCollector.recordDataQualityScore(batchId, qualityScore);
        
        // Then: The validation counter should be incremented
        Counter validationCounter = meterRegistry.find("business.data_quality.validations").counter();
        assertNotNull(validationCounter);
        assertEquals(1.0, validationCounter.count());
        
        // And: The quality score gauge should reflect the correct value
        Gauge scoreGauge = meterRegistry.find("business.data_quality.score.current")
            .tag("batch.id", batchId)
            .gauge();
        assertNotNull(scoreGauge);
        assertEquals(qualityScore, scoreGauge.value(), 0.001);
        
        // And: Business context tags should be present
        assertEquals("data-quality", validationCounter.getId().getTag("component"));
    }

    @Test
    void riskCalculationMetricsAreAccurate() {
        // Given
        String portfolioId = "portfolio-456";
        int exposureCount = 1500;
        int calculationTimeMs = 2500;
        
        // When: Recording a risk calculation completion
        metricsCollector.recordRiskCalculationCompletion(portfolioId, exposureCount, calculationTimeMs);
        
        // Then: The calculation counter should be incremented
        Counter calculationCounter = meterRegistry.find("business.risk_calculation.calculations").counter();
        assertNotNull(calculationCounter);
        assertEquals(1.0, calculationCounter.count());
        
        // And: The exposure count gauge should reflect the correct value
        Gauge exposureGauge = meterRegistry.find("business.risk_calculation.exposures.count")
            .tag("portfolio.id", portfolioId)
            .gauge();
        assertNotNull(exposureGauge);
        assertEquals(exposureCount, exposureGauge.value());
        
        // And: Business context tags should be present
        assertEquals("risk-calculation", calculationCounter.getId().getTag("component"));
        assertEquals(portfolioId, exposureGauge.getId().getTag("portfolio.id"));
    }

    @Test
    void batchProcessingMetricsAreAccurate() {
        // Given
        String batchId = "batch-789";
        int recordCount = 50000;
        String processingStatus = "COMPLETED";
        
        // When: Recording batch processing metrics
        metricsCollector.recordBatchProcessing(batchId, recordCount, processingStatus);
        
        // Then: The batch processing counter should be incremented
        Counter batchCounter = meterRegistry.find("business.batch.processing").counter();
        assertNotNull(batchCounter);
        assertEquals(1.0, batchCounter.count());
        
        // And: The record count gauge should reflect the correct value
        Gauge recordGauge = meterRegistry.find("business.batch.records.count")
            .tag("batch.id", batchId)
            .tag("status", processingStatus)
            .gauge();
        assertNotNull(recordGauge);
        assertEquals(recordCount, recordGauge.value());
        
        // And: Business context tags should be present
        assertEquals("ingestion", batchCounter.getId().getTag("component"));
        assertEquals(batchId, recordGauge.getId().getTag("batch.id"));
        assertEquals(processingStatus, recordGauge.getId().getTag("status"));
    }

    @Test
    void authenticationMetricsAreAccurate() {
        // Given
        String authMethod = "JWT";
        boolean success = true;
        String userRole = "ADMIN";
        
        // When: Recording authentication metrics
        metricsCollector.recordAuthentication(authMethod, success, userRole);
        
        // Then: The authentication counter should be incremented with correct tags
        Counter authCounter = meterRegistry.find("business.authentication.attempts")
            .tag("auth.method", authMethod)
            .tag("success", String.valueOf(success))
            .tag("user.role", userRole)
            .counter();
        assertNotNull(authCounter);
        assertEquals(1.0, authCounter.count());
        
        // And: Business context tags should be present
        assertEquals("iam", authCounter.getId().getTag("component"));
        assertEquals(authMethod, authCounter.getId().getTag("auth.method"));
        assertEquals(String.valueOf(success), authCounter.getId().getTag("success"));
        assertEquals(userRole, authCounter.getId().getTag("user.role"));
    }

    @Test
    void customBusinessEventMetricsAreAccurate() {
        // Given
        String eventType = "data-validation";
        Map<String, String> tags = new HashMap<>();
        tags.put("module", "data-quality");
        tags.put("severity", "high");
        
        // When: Recording custom business event
        metricsCollector.recordCustomBusinessEvent(eventType, tags);
        
        // Then: The custom events counter should be incremented
        Counter customCounter = meterRegistry.find("business.custom.events")
            .tag("event.type", eventType)
            .counter();
        assertNotNull(customCounter);
        assertEquals(1.0, customCounter.count());
        
        // And: Event type tag should be present
        assertEquals(eventType, customCounter.getId().getTag("event.type"));
    }

    @Test
    void multipleOperationsAccumulateCorrectly() {
        // Given
        String batchId = "multi-batch";
        int operationCount = 5;
        
        // When: Recording multiple data quality operations for the same batch
        for (int i = 0; i < operationCount; i++) {
            metricsCollector.recordDataQualityScore(batchId, 0.8);
        }
        
        // Then: The validation counter should reflect all operations
        Counter validationCounter = meterRegistry.find("business.data_quality.validations").counter();
        assertNotNull(validationCounter);
        assertEquals(operationCount, validationCounter.count());
        
        // And: The latest quality score should be recorded
        Gauge scoreGauge = meterRegistry.find("business.data_quality.score.current")
            .tag("batch.id", batchId)
            .gauge();
        assertNotNull(scoreGauge);
        assertEquals(0.8, scoreGauge.value(), 0.001);
    }

    @Test
    void activeProcessCountIsAccurate() {
        // Given: No active processes initially
        assertThat(metricsCollector.getActiveProcessCount()).isEqualTo(0);
        
        // When: Starting batch processing
        metricsCollector.recordBatchProcessing("batch1", 100, "PROCESSING");
        metricsCollector.recordBatchProcessing("batch2", 200, "PROCESSING");
        
        // Then: Active process count should reflect active batches
        assertThat(metricsCollector.getActiveProcessCount()).isEqualTo(2);
        
        // When: Completing one batch
        metricsCollector.recordBatchProcessing("batch1", 100, "COMPLETED");
        
        // Then: Active process count should decrease
        assertThat(metricsCollector.getActiveProcessCount()).isEqualTo(1);
    }

    @Test
    void averageQualityScoreIsAccurate() {
        // Given: No quality scores initially
        assertThat(metricsCollector.getAverageQualityScore()).isEqualTo(0.0);
        
        // When: Recording quality scores
        metricsCollector.recordDataQualityScore("batch1", 0.8);
        metricsCollector.recordDataQualityScore("batch2", 0.6);
        metricsCollector.recordDataQualityScore("batch3", 1.0);
        
        // Then: Average quality score should be calculated correctly
        double expectedAverage = (0.8 + 0.6 + 1.0) / 3.0;
        assertThat(metricsCollector.getAverageQualityScore()).isCloseTo(expectedAverage, within(0.001));
    }

}