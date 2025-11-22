package com.bcbs239.regtech.reportgeneration.presentation.health;

import com.bcbs239.regtech.reportgeneration.application.coordination.BatchEventTracker;
import com.bcbs239.regtech.reportgeneration.domain.generation.IGeneratedReportRepository;
import com.bcbs239.regtech.reportgeneration.domain.storage.IReportStorageService;
import com.bcbs239.regtech.reportgeneration.presentation.health.ReportGenerationHealthChecker.HealthCheckResult;
import com.bcbs239.regtech.reportgeneration.presentation.health.ReportGenerationHealthChecker.ModuleHealthResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReportGenerationHealthChecker.
 * Tests health check logic for all components.
 * 
 * Requirements: 24.1, 24.3, 24.4
 */
@ExtendWith(MockitoExtension.class)
class ReportGenerationHealthCheckerTest {
    
    @Mock
    private IGeneratedReportRepository reportRepository;
    
    @Mock
    private IReportStorageService storageService;
    
    @Mock
    private BatchEventTracker eventTracker;
    
    @Mock
    private ThreadPoolTaskExecutor asyncExecutor;
    
    @Mock
    private ThreadPoolExecutor threadPoolExecutor;
    
    private ReportGenerationHealthChecker healthChecker;
    
    @BeforeEach
    void setUp() {
        healthChecker = new ReportGenerationHealthChecker(
            reportRepository,
            storageService,
            eventTracker,
            asyncExecutor
        );
    }
    
    @Test
    void checkDatabaseHealth_WhenRepositoryAvailable_ReturnsUp() {
        // Act
        HealthCheckResult result = healthChecker.checkDatabaseHealth();
        
        // Assert
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.message()).contains("Database is accessible");
        assertThat(result.isHealthy()).isTrue();
    }
    
    @Test
    void checkS3Accessibility_WhenServiceAvailable_ReturnsUp() {
        // Act
        HealthCheckResult result = healthChecker.checkS3Accessibility();
        
        // Assert
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.message()).contains("S3 storage service is available");
        assertThat(result.isHealthy()).isTrue();
    }
    
    @Test
    void checkEventTrackerState_WhenNoPendingEvents_ReturnsUp() {
        // Arrange
        when(eventTracker.getTrackedBatchCount()).thenReturn(0);
        
        // Act
        HealthCheckResult result = healthChecker.checkEventTrackerState();
        
        // Assert
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.message()).contains("Event tracker operating normally");
        assertThat(result.details()).containsEntry("pendingEvents", 0);
    }
    
    @Test
    void checkEventTrackerState_WhenManyPendingEvents_ReturnsWarn() {
        // Arrange
        when(eventTracker.getTrackedBatchCount()).thenReturn(15);
        
        // Act
        HealthCheckResult result = healthChecker.checkEventTrackerState();
        
        // Assert
        assertThat(result.status()).isEqualTo("WARN");
        assertThat(result.message()).contains("High number of pending events");
        assertThat(result.details()).containsEntry("pendingEvents", 15);
    }
    
    @Test
    void checkEventTrackerState_WhenTooManyPendingEvents_ReturnsDown() {
        // Arrange
        when(eventTracker.getTrackedBatchCount()).thenReturn(60);
        
        // Act
        HealthCheckResult result = healthChecker.checkEventTrackerState();
        
        // Assert
        assertThat(result.status()).isEqualTo("DOWN");
        assertThat(result.message()).contains("Too many pending events");
        assertThat(result.details()).containsEntry("pendingEvents", 60);
    }
    
    @Test
    void checkAsyncExecutorQueueSize_WhenQueueEmpty_ReturnsUp() {
        // Arrange
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(100);
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(threadPoolExecutor.getQueue()).thenReturn(queue);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(1);
        when(asyncExecutor.getPoolSize()).thenReturn(2);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(5);
        
        // Act
        HealthCheckResult result = healthChecker.checkAsyncExecutorQueueSize();
        
        // Assert
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.message()).contains("Async executor operating normally");
        assertThat(result.details()).containsEntry("queueSize", 0);
    }
    
    @Test
    void checkAsyncExecutorQueueSize_WhenQueueHalfFull_ReturnsWarn() {
        // Arrange
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(100);
        // Add 60 items to queue (60% full)
        for (int i = 0; i < 60; i++) {
            queue.add(() -> {});
        }
        
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(threadPoolExecutor.getQueue()).thenReturn(queue);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(3);
        when(asyncExecutor.getPoolSize()).thenReturn(4);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(5);
        
        // Act
        HealthCheckResult result = healthChecker.checkAsyncExecutorQueueSize();
        
        // Assert
        assertThat(result.status()).isEqualTo("WARN");
        assertThat(result.message()).contains("Queue utilization high");
        assertThat(result.details()).containsEntry("queueSize", 60);
    }
    
    @Test
    void checkAsyncExecutorQueueSize_WhenQueueNearlyFull_ReturnsDown() {
        // Arrange
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(100);
        // Add 85 items to queue (85% full)
        for (int i = 0; i < 85; i++) {
            queue.add(() -> {});
        }
        
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(threadPoolExecutor.getQueue()).thenReturn(queue);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(5);
        when(asyncExecutor.getPoolSize()).thenReturn(5);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(5);
        
        // Act
        HealthCheckResult result = healthChecker.checkAsyncExecutorQueueSize();
        
        // Assert
        assertThat(result.status()).isEqualTo("DOWN");
        assertThat(result.message()).contains("Queue nearly full");
        assertThat(result.details()).containsEntry("queueSize", 85);
    }
    
    @Test
    void checkModuleHealth_WhenAllComponentsHealthy_ReturnsUp() {
        // Arrange
        when(eventTracker.getTrackedBatchCount()).thenReturn(0);
        
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(100);
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(threadPoolExecutor.getQueue()).thenReturn(queue);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(1);
        when(asyncExecutor.getPoolSize()).thenReturn(2);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(5);
        
        // Act
        ModuleHealthResult result = healthChecker.checkModuleHealth();
        
        // Assert
        assertThat(result.overallStatus()).isEqualTo("UP");
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.databaseHealth().status()).isEqualTo("UP");
        assertThat(result.s3Health().status()).isEqualTo("UP");
        assertThat(result.eventTrackerHealth().status()).isEqualTo("UP");
        assertThat(result.asyncExecutorHealth().status()).isEqualTo("UP");
    }
    
    @Test
    void checkModuleHealth_WhenOneComponentWarn_ReturnsWarn() {
        // Arrange
        when(eventTracker.getTrackedBatchCount()).thenReturn(15); // WARN threshold
        
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(100);
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(threadPoolExecutor.getQueue()).thenReturn(queue);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(1);
        when(asyncExecutor.getPoolSize()).thenReturn(2);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(5);
        
        // Act
        ModuleHealthResult result = healthChecker.checkModuleHealth();
        
        // Assert
        assertThat(result.overallStatus()).isEqualTo("WARN");
        assertThat(result.eventTrackerHealth().status()).isEqualTo("WARN");
    }
    
    @Test
    void checkModuleHealth_WhenOneComponentDown_ReturnsDown() {
        // Arrange
        when(eventTracker.getTrackedBatchCount()).thenReturn(60); // DOWN threshold
        
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(100);
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(threadPoolExecutor.getQueue()).thenReturn(queue);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(1);
        when(asyncExecutor.getPoolSize()).thenReturn(2);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(5);
        
        // Act
        ModuleHealthResult result = healthChecker.checkModuleHealth();
        
        // Assert
        assertThat(result.overallStatus()).isEqualTo("DOWN");
        assertThat(result.eventTrackerHealth().status()).isEqualTo("DOWN");
    }
    
    @Test
    void healthCheckResult_ToMap_ReturnsCorrectStructure() {
        // Arrange
        HealthCheckResult result = new HealthCheckResult(
            "UP",
            "Test message",
            java.util.Map.of("key", "value")
        );
        
        // Act
        var map = result.toMap();
        
        // Assert
        assertThat(map).containsEntry("status", "UP");
        assertThat(map).containsEntry("message", "Test message");
        assertThat(map).containsKey("details");
    }
    
    @Test
    void moduleHealthResult_ToResponseMap_ReturnsCorrectStructure() {
        // Arrange
        when(eventTracker.getTrackedBatchCount()).thenReturn(0);
        
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(100);
        when(asyncExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(threadPoolExecutor.getQueue()).thenReturn(queue);
        when(asyncExecutor.getQueueCapacity()).thenReturn(100);
        when(asyncExecutor.getActiveCount()).thenReturn(1);
        when(asyncExecutor.getPoolSize()).thenReturn(2);
        when(asyncExecutor.getMaxPoolSize()).thenReturn(5);
        
        // Act
        ModuleHealthResult result = healthChecker.checkModuleHealth();
        var responseMap = result.toResponseMap();
        
        // Assert
        assertThat(responseMap).containsEntry("module", "report-generation");
        assertThat(responseMap).containsEntry("status", "UP");
        assertThat(responseMap).containsKey("timestamp");
        assertThat(responseMap).containsKey("checkDuration");
        assertThat(responseMap).containsKey("components");
        assertThat(responseMap).containsEntry("version", "1.0.0");
    }
}
