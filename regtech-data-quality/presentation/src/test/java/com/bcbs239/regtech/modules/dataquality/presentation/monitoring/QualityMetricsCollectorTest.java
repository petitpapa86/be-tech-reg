package com.bcbs239.regtech.modules.dataquality.presentation.monitoring;

import com.bcbs239.regtech.dataquality.presentation.monitoring.QualityMetricsCollector;
import com.bcbs239.regtech.dataquality.presentation.monitoring.QualityMetricsCollector.QualityMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QualityMetricsCollector.
 */
class QualityMetricsCollectorTest {
    
    private QualityMetricsCollector metricsCollector;
    
    @BeforeEach
    void setUp() {
        metricsCollector = new QualityMetricsCollector();
    }
    
    @Test
    void shouldCollectMetrics() {
        // When
        QualityMetrics metrics = metricsCollector.collectMetrics();
        
        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.timestamp()).isNotNull();
        assertThat(metrics.jvmMetrics()).isNotNull();
        assertThat(metrics.moduleMetrics()).isNotNull();
    }
    
    @Test
    void shouldIncludeJvmMetrics() {
        // When
        QualityMetrics metrics = metricsCollector.collectMetrics();
        
        // Then
        Map<String, Object> jvmMetrics = metrics.jvmMetrics();
        assertThat(jvmMetrics).containsKeys(
            "totalMemory", "freeMemory", "usedMemory", 
            "memoryUsagePercent", "availableProcessors", "maxMemory"
        );
    }
    
    @Test
    void shouldIncludeModuleMetrics() {
        // When
        QualityMetrics metrics = metricsCollector.collectMetrics();
        
        // Then
        Map<String, Object> moduleMetrics = metrics.moduleMetrics();
        assertThat(moduleMetrics).containsKeys(
            "totalReportsProcessed", "averageProcessingTime", "errorRate",
            "lastProcessedBatch", "validationRulesLoaded", "cacheHitRate", "throughputPerHour"
        );
    }
    
    @Test
    void shouldFormatResponseMap() {
        // Given
        QualityMetrics metrics = metricsCollector.collectMetrics();
        
        // When
        Map<String, Object> responseMap = metrics.toResponseMap();
        
        // Then
        assertThat(responseMap).containsKeys("module", "timestamp", "metrics");
        assertThat(responseMap.get("module")).isEqualTo("data-quality");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metricsMap = (Map<String, Object>) responseMap.get("metrics");
        assertThat(metricsMap).containsKeys("jvm", "dataQuality");
    }
}