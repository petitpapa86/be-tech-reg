package com.bcbs239.regtech.reportgeneration.presentation.monitoring;

import com.bcbs239.regtech.reportgeneration.application.coordination.BatchEventTracker;
import com.bcbs239.regtech.reportgeneration.application.generation.ReportGenerationMetrics;
import com.bcbs239.regtech.reportgeneration.domain.generation.IGeneratedReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReportGenerationMetricsCollector.
 * Validates metrics collection for JVM, module, and resource metrics.
 */
@ExtendWith(MockitoExtension.class)
class ReportGenerationMetricsCollectorTest {
    
    @Mock
    private ReportGenerationMetrics reportGenerationMetrics;
    
    @Mock
    private BatchEventTracker eventTracker;
    
    @Mock
    private ThreadPoolTaskExecutor asyncExecutor;
    
    @Mock
    private DataSource dataSource;
    
    @Mock
    private IGeneratedReportRepository reportRepository;
    
    @Mock
    private Connection connection;
    
    private ReportGenerationMetricsCollector metricsCollector;
    
    @BeforeEach
    void setUp() {
        metricsCollector = new ReportGenerationMetricsCollector(
            reportGenerationMetrics,
            eventTracker,
            asyncExecutor,
            dataSource,
            reportRepository
        );
    }
    
    @Test
    void collectMetrics_shouldReturnCompleteMetricsSnapshot() {
        // Arrange
        ReportGenerationMetrics.MetricsSnapshot mockSnapshot = new ReportGenerationMetrics.MetricsSnapshot(
            100L,  // totalReportsGenerated
            5L,    // totalReportsFailed
            2L,    // totalReportsPartial
            3L,    // totalDuplicatesSkipped
            5000.0, // averageGenerationTimeMillis
            800.0,  // averageDataFetchTimeMillis
            2000.0, // averageHtmlGenerationTimeMillis
            800.0,  // averageXbrlGenerationTimeMillis
            4.76,   // failureRatePercent
            1.96,   // partialRatePercent
            2L,     // activeGenerations
            Map.of("S3_UPLOAD_FAILED", 3L, "XBRL_VALIDATION_FAILED", 2L),
            Instant.now()
        );
        
        when(reportGenerationMetrics.getSnapshot()).thenReturn(mockSnapshot);
        when(eventTracker.getTrackedBatchCount()).thenReturn(5);
        
        // Mock async executor
        java.util.concurrent.ThreadPoolExecutor mockExecutor = 
            new java.util.concurrent.ThreadPoolExecutor(
                5, 10, 60, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(100)
            );
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(mockExecutor);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(2);
        when(asyncExecutor.getPoolSize()).thenReturn(5);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(10);
        when(asyncExecutor.getCorePoolSize()).thenReturn(5);
        
        // Act
        ReportGenerationMetricsCollector.ReportMetrics metrics = metricsCollector.collectMetrics();
        
        // Assert
        assertThat(metrics).isNotNull();
        assertThat(metrics.timestamp()).isNotNull();
        
        // Verify JVM metrics
        assertThat(metrics.jvmMetrics()).containsKeys(
            "totalMemory", "freeMemory", "usedMemory", 
            "memoryUsagePercent", "availableProcessors", "maxMemory"
        );
        
        // Verify module metrics
        assertThat(metrics.moduleMetrics()).containsKeys(
            "totalReportsGenerated", "totalReportsFailed", "totalReportsPartial",
            "averageGenerationTimeMillis", "failureRatePercent"
        );
        
        // Verify resource metrics
        assertThat(metrics.resourceMetrics()).containsKeys(
            "databaseConnectionPool", "asyncExecutor", "eventTracker", "circuitBreaker"
        );
        
        // Verify response map structure
        Map<String, Object> responseMap = metrics.toResponseMap();
        assertThat(responseMap).containsKeys("module", "timestamp", "metrics");
        assertThat(responseMap.get("module")).isEqualTo("report-generation");
    }
    
    @Test
    void collectMetrics_shouldIncludeAsyncExecutorMetrics() {
        // Arrange
        ReportGenerationMetrics.MetricsSnapshot mockSnapshot = new ReportGenerationMetrics.MetricsSnapshot(
            10L, 1L, 0L, 0L, 5000.0, 800.0, 2000.0, 800.0, 9.09, 0.0, 1L,
            Map.of(), Instant.now()
        );
        when(reportGenerationMetrics.getSnapshot()).thenReturn(mockSnapshot);
        when(eventTracker.getTrackedBatchCount()).thenReturn(2);
        
        // Mock async executor with specific values
        java.util.concurrent.ThreadPoolExecutor mockExecutor = 
            new java.util.concurrent.ThreadPoolExecutor(
                5, 10, 60, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(100)
            );
        // Add some tasks to the queue
        for (int i = 0; i < 30; i++) {
            mockExecutor.execute(() -> {});
        }
        
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(mockExecutor);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(3);
        when(asyncExecutor.getPoolSize()).thenReturn(5);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(10);
        when(asyncExecutor.getCorePoolSize()).thenReturn(5);
        
        // Act
        ReportGenerationMetricsCollector.ReportMetrics metrics = metricsCollector.collectMetrics();
        
        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> asyncMetrics = (Map<String, Object>) metrics.resourceMetrics().get("asyncExecutor");
        
        assertThat(asyncMetrics).isNotNull();
        assertThat(asyncMetrics.get("queueCapacity")).isEqualTo(100);
        assertThat(asyncMetrics.get("activeThreads")).isEqualTo(3);
        assertThat(asyncMetrics.get("poolSize")).isEqualTo(5);
        assertThat(asyncMetrics.get("maxPoolSize")).isEqualTo(10);
        assertThat(asyncMetrics.get("corePoolSize")).isEqualTo(5);
        assertThat(asyncMetrics.get("status")).isEqualTo("active");
        
        // Verify queue utilization calculation
        assertThat(asyncMetrics.get("queueUtilizationPercent")).isInstanceOf(Double.class);
        assertThat(asyncMetrics.get("poolUtilizationPercent")).isInstanceOf(Double.class);
    }
    
    @Test
    void collectMetrics_shouldIncludeEventTrackerMetrics() {
        // Arrange
        ReportGenerationMetrics.MetricsSnapshot mockSnapshot = new ReportGenerationMetrics.MetricsSnapshot(
            10L, 1L, 0L, 0L, 5000.0, 800.0, 2000.0, 800.0, 9.09, 0.0, 1L,
            Map.of(), Instant.now()
        );
        when(reportGenerationMetrics.getSnapshot()).thenReturn(mockSnapshot);
        when(eventTracker.getTrackedBatchCount()).thenReturn(15);
        
        java.util.concurrent.ThreadPoolExecutor mockExecutor = 
            new java.util.concurrent.ThreadPoolExecutor(
                5, 10, 60, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(100)
            );
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(mockExecutor);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(2);
        when(asyncExecutor.getPoolSize()).thenReturn(5);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(10);
        when(asyncExecutor.getCorePoolSize()).thenReturn(5);
        
        // Act
        ReportGenerationMetricsCollector.ReportMetrics metrics = metricsCollector.collectMetrics();
        
        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> trackerMetrics = (Map<String, Object>) metrics.resourceMetrics().get("eventTracker");
        
        assertThat(trackerMetrics).isNotNull();
        assertThat(trackerMetrics.get("pendingEvents")).isEqualTo(15);
        assertThat(trackerMetrics.get("status")).isEqualTo("normal");
    }
    
    @Test
    void collectMetrics_shouldHandleNullAsyncExecutor() {
        // Arrange
        ReportGenerationMetrics.MetricsSnapshot mockSnapshot = new ReportGenerationMetrics.MetricsSnapshot(
            10L, 1L, 0L, 0L, 5000.0, 800.0, 2000.0, 800.0, 9.09, 0.0, 1L,
            Map.of(), Instant.now()
        );
        when(reportGenerationMetrics.getSnapshot()).thenReturn(mockSnapshot);
        when(eventTracker.getTrackedBatchCount()).thenReturn(2);
        
        // Create collector with null executor
        ReportGenerationMetricsCollector collectorWithNullExecutor = 
            new ReportGenerationMetricsCollector(
                reportGenerationMetrics,
                eventTracker,
                null,  // null executor
                dataSource,
                reportRepository
            );
        
        // Act
        ReportGenerationMetricsCollector.ReportMetrics metrics = collectorWithNullExecutor.collectMetrics();
        
        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> asyncMetrics = (Map<String, Object>) metrics.resourceMetrics().get("asyncExecutor");
        
        assertThat(asyncMetrics).isNotNull();
        assertThat(asyncMetrics.get("status")).isEqualTo("unavailable");
        assertThat(asyncMetrics.get("error")).isEqualTo("Executor not configured");
    }
    
    @Test
    void collectMetrics_shouldHandleNullEventTracker() {
        // Arrange
        ReportGenerationMetrics.MetricsSnapshot mockSnapshot = new ReportGenerationMetrics.MetricsSnapshot(
            10L, 1L, 0L, 0L, 5000.0, 800.0, 2000.0, 800.0, 9.09, 0.0, 1L,
            Map.of(), Instant.now()
        );
        when(reportGenerationMetrics.getSnapshot()).thenReturn(mockSnapshot);
        
        java.util.concurrent.ThreadPoolExecutor mockExecutor = 
            new java.util.concurrent.ThreadPoolExecutor(
                5, 10, 60, java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(100)
            );
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(mockExecutor);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(2);
        when(asyncExecutor.getPoolSize()).thenReturn(5);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(10);
        when(asyncExecutor.getCorePoolSize()).thenReturn(5);
        
        // Create collector with null tracker
        ReportGenerationMetricsCollector collectorWithNullTracker = 
            new ReportGenerationMetricsCollector(
                reportGenerationMetrics,
                null,  // null tracker
                asyncExecutor,
                dataSource,
                reportRepository
            );
        
        // Act
        ReportGenerationMetricsCollector.ReportMetrics metrics = collectorWithNullTracker.collectMetrics();
        
        // Assert
        @SuppressWarnings("unchecked")
        Map<String, Object> trackerMetrics = (Map<String, Object>) metrics.resourceMetrics().get("eventTracker");
        
        assertThat(trackerMetrics).isNotNull();
        assertThat(trackerMetrics.get("status")).isEqualTo("unavailable");
        assertThat(trackerMetrics.get("error")).isEqualTo("Event tracker not configured");
    }
}
