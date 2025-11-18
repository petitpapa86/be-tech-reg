package com.bcbs239.regtech.riskcalculation.presentation.monitoring;

import com.bcbs239.regtech.riskcalculation.application.monitoring.PerformanceMetrics;
import com.bcbs239.regtech.riskcalculation.domain.calculation.IBatchSummaryRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Collector for performance metrics and statistics.
 * Gathers JVM metrics and module-specific performance data.
 */
@Component
public class RiskCalculationMetricsCollector {
    
    private final IBatchSummaryRepository batchSummaryRepository;
    private final PerformanceMetrics performanceMetrics;
    
    public RiskCalculationMetricsCollector(
            IBatchSummaryRepository batchSummaryRepository,
            PerformanceMetrics performanceMetrics) {
        this.batchSummaryRepository = batchSummaryRepository;
        this.performanceMetrics = performanceMetrics;
    }
    
    /**
     * Collects comprehensive performance metrics and statistics.
     */
    public RiskCalculationMetrics collectMetrics() {
        Map<String, Object> jvmMetrics = collectJvmMetrics();
        Map<String, Object> moduleMetrics = collectModuleMetrics();
        
        return new RiskCalculationMetrics(
            Instant.now(),
            jvmMetrics,
            moduleMetrics
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
     * Collects module-specific metrics from the PerformanceMetrics component.
     */
    private Map<String, Object> collectModuleMetrics() {
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        return snapshot.toMap();
    }
    
    /**
     * Record for risk calculation metrics data.
     */
    public record RiskCalculationMetrics(
        Instant timestamp,
        Map<String, Object> jvmMetrics,
        Map<String, Object> moduleMetrics
    ) {
        public Map<String, Object> toResponseMap() {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("jvm", jvmMetrics);
            metrics.put("riskCalculation", moduleMetrics);
            
            return Map.of(
                "module", "risk-calculation",
                "timestamp", timestamp.toString(),
                "metrics", metrics
            );
        }
    }
}
