package com.bcbs239.regtech.reportgeneration.presentation.monitoring;

import com.bcbs239.regtech.reportgeneration.application.coordination.BatchEventTracker;
import com.bcbs239.regtech.reportgeneration.application.generation.ReportGenerationMetrics;
import com.bcbs239.regtech.reportgeneration.domain.generation.IGeneratedReportRepository;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Collector for performance metrics and statistics for report generation module.
 * Gathers JVM metrics, module-specific performance data, and resource usage.
 * 
 * Requirements: 16.1, 16.2, 16.3, 19.1, 19.2, 19.3
 */
@Component
public class ReportGenerationMetricsCollector {
    
    private final ReportGenerationMetrics reportGenerationMetrics;
    private final BatchEventTracker eventTracker;
    private final ThreadPoolTaskExecutor asyncExecutor;
    private final DataSource dataSource;
    private final IGeneratedReportRepository reportRepository;
    
    public ReportGenerationMetricsCollector(
            ReportGenerationMetrics reportGenerationMetrics,
            BatchEventTracker eventTracker,
            ThreadPoolTaskExecutor asyncExecutor,
            DataSource dataSource,
            IGeneratedReportRepository reportRepository) {
        this.reportGenerationMetrics = reportGenerationMetrics;
        this.eventTracker = eventTracker;
        this.asyncExecutor = asyncExecutor;
        this.dataSource = dataSource;
        this.reportRepository = reportRepository;
    }
    
    /**
     * Collects comprehensive performance metrics and statistics.
     */
    public ReportMetrics collectMetrics() {
        Map<String, Object> jvmMetrics = collectJvmMetrics();
        Map<String, Object> moduleMetrics = collectModuleMetrics();
        Map<String, Object> resourceMetrics = collectResourceMetrics();
        
        return new ReportMetrics(
            Instant.now(),
            jvmMetrics,
            moduleMetrics,
            resourceMetrics
        );
    }
    
    /**
     * Collects JVM-related metrics.
     */
    private Map<String, Object> collectJvmMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return Map.of(
            "totalMemory", totalMemory,
            "freeMemory", freeMemory,
            "usedMemory", usedMemory,
            "memoryUsagePercent", (double) usedMemory / totalMemory * 100.0,
            "availableProcessors", runtime.availableProcessors(),
            "maxMemory", runtime.maxMemory()
        );
    }
    
    /**
     * Collects module-specific metrics from the ReportGenerationMetrics component.
     * Includes timers for operations and counters for success/failure/partial.
     */
    private Map<String, Object> collectModuleMetrics() {
        ReportGenerationMetrics.MetricsSnapshot snapshot = reportGenerationMetrics.getSnapshot();
        return snapshot.toMap();
    }
    
    /**
     * Collects resource usage metrics.
     * Includes database connection pool, async executor, event tracker, and circuit breaker state.
     */
    private Map<String, Object> collectResourceMetrics() {
        Map<String, Object> resources = new HashMap<>();
        
        // Database connection pool metrics
        resources.put("databaseConnectionPool", collectDatabasePoolMetrics());
        
        // Async executor metrics
        resources.put("asyncExecutor", collectAsyncExecutorMetrics());
        
        // Event tracker metrics
        resources.put("eventTracker", collectEventTrackerMetrics());
        
        // Circuit breaker state (placeholder - would be collected from Resilience4j)
        resources.put("circuitBreaker", collectCircuitBreakerMetrics());
        
        return resources;
    }
    
    /**
     * Collects database connection pool metrics.
     * Gauges for active and idle connections.
     */
    private Map<String, Object> collectDatabasePoolMetrics() {
        Map<String, Object> poolMetrics = new HashMap<>();
        
        try {
            // Test connection availability
            try (Connection conn = dataSource.getConnection()) {
                poolMetrics.put("status", "available");
                poolMetrics.put("connectionTest", "passed");
            }
            
            // Note: Actual pool metrics (active/idle connections) would require
            // HikariCP-specific APIs or JMX beans. This is a simplified version.
            poolMetrics.put("note", "Detailed pool metrics require HikariCP MBean access");
            
        } catch (SQLException e) {
            poolMetrics.put("status", "unavailable");
            poolMetrics.put("error", e.getMessage());
        }
        
        return poolMetrics;
    }
    
    /**
     * Collects async executor metrics.
     * Gauges for queue size, active threads, and pool utilization.
     */
    private Map<String, Object> collectAsyncExecutorMetrics() {
        if (asyncExecutor == null) {
            return Map.of("status", "unavailable", "error", "Executor not configured");
        }
        
        int queueSize = asyncExecutor.getThreadPoolExecutor().getQueue().size();
        int queueCapacity = asyncExecutor.getQueueCapacity();
        int activeThreads = asyncExecutor.getActiveCount();
        int poolSize = asyncExecutor.getPoolSize();
        int maxPoolSize = asyncExecutor.getMaxPoolSize();
        int corePoolSize = asyncExecutor.getCorePoolSize();
        
        double queueUtilization = queueCapacity > 0 
            ? (double) queueSize / queueCapacity * 100.0 
            : 0.0;
        
        double poolUtilization = maxPoolSize > 0
            ? (double) poolSize / maxPoolSize * 100.0
            : 0.0;
        
        return Map.of(
            "queueSize", queueSize,
            "queueCapacity", queueCapacity,
            "queueUtilizationPercent", queueUtilization,
            "activeThreads", activeThreads,
            "poolSize", poolSize,
            "corePoolSize", corePoolSize,
            "maxPoolSize", maxPoolSize,
            "poolUtilizationPercent", poolUtilization,
            "status", "active"
        );
    }
    
    /**
     * Collects event tracker metrics.
     * Gauges for pending events and tracker state.
     */
    private Map<String, Object> collectEventTrackerMetrics() {
        if (eventTracker == null) {
            return Map.of("status", "unavailable", "error", "Event tracker not configured");
        }
        
        int trackedBatchCount = eventTracker.getTrackedBatchCount();
        
        return Map.of(
            "pendingEvents", trackedBatchCount,
            "status", trackedBatchCount > 50 ? "overloaded" : "normal"
        );
    }
    
    /**
     * Collects circuit breaker metrics.
     * Gauges for circuit breaker state (open/closed/half-open).
     * 
     * Note: This is a placeholder. Actual implementation would integrate with
     * Resilience4j CircuitBreakerRegistry to get real-time state.
     */
    private Map<String, Object> collectCircuitBreakerMetrics() {
        // In a real implementation, this would query Resilience4j:
        // CircuitBreakerRegistry registry = ...;
        // CircuitBreaker cb = registry.circuitBreaker("s3ReportStorage");
        // return Map.of("state", cb.getState().name(), ...);
        
        return Map.of(
            "s3Storage", Map.of(
                "state", "CLOSED",
                "failureRate", 0.0,
                "slowCallRate", 0.0,
                "note", "Actual metrics require Resilience4j integration"
            )
        );
    }
    
    /**
     * Record for report generation metrics data.
     */
    public record ReportMetrics(
        Instant timestamp,
        Map<String, Object> jvmMetrics,
        Map<String, Object> moduleMetrics,
        Map<String, Object> resourceMetrics
    ) {
        public Map<String, Object> toResponseMap() {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("jvm", jvmMetrics);
            metrics.put("reportGeneration", moduleMetrics);
            metrics.put("resources", resourceMetrics);
            
            return Map.of(
                "module", "report-generation",
                "timestamp", timestamp.toString(),
                "metrics", metrics
            );
        }
    }
}
