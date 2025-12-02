package com.bcbs239.regtech.riskcalculation.presentation.monitoring;

import com.bcbs239.regtech.riskcalculation.application.monitoring.PerformanceMetrics;
import com.bcbs239.regtech.riskcalculation.domain.calculation.IBatchSummaryRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.ExposureRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.MitigationRepository;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Collector for performance metrics and statistics.
 * Gathers JVM metrics and module-specific performance data.
 * Tracks metrics for new bounded context architecture.
 * 
 * Requirements: 5.2, 5.3, 5.5
 */
@Component
public class RiskCalculationMetricsCollector {
    
    private final IBatchSummaryRepository batchSummaryRepository;
    private final PortfolioAnalysisRepository portfolioAnalysisRepository;
    private final ExposureRepository exposureRepository;
    private final MitigationRepository mitigationRepository;
    private final PerformanceMetrics performanceMetrics;
    
    public RiskCalculationMetricsCollector(
            IBatchSummaryRepository batchSummaryRepository,
            PortfolioAnalysisRepository portfolioAnalysisRepository,
            ExposureRepository exposureRepository,
            MitigationRepository mitigationRepository,
            PerformanceMetrics performanceMetrics) {
        this.batchSummaryRepository = batchSummaryRepository;
        this.portfolioAnalysisRepository = portfolioAnalysisRepository;
        this.exposureRepository = exposureRepository;
        this.mitigationRepository = mitigationRepository;
        this.performanceMetrics = performanceMetrics;
    }
    
    /**
     * Collects comprehensive performance metrics and statistics.
     * Includes bounded context metrics for portfolio analysis, exposures, and mitigations.
     */
    public RiskCalculationMetrics collectMetrics() {
        Map<String, Object> jvmMetrics = collectJvmMetrics();
        Map<String, Object> moduleMetrics = collectModuleMetrics();
        Map<String, Object> boundedContextMetrics = collectBoundedContextMetrics();
        
        return new RiskCalculationMetrics(
            Instant.now(),
            jvmMetrics,
            moduleMetrics,
            boundedContextMetrics
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
     * Collects bounded context specific metrics.
     * Tracks portfolio analysis, exposures processed, and calculation times.
     */
    private Map<String, Object> collectBoundedContextMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Portfolio Analysis metrics
        Map<String, Object> portfolioMetrics = new HashMap<>();
        portfolioMetrics.put("repositoryAvailable", portfolioAnalysisRepository != null);
        portfolioMetrics.put("description", "Portfolio analysis aggregates and concentration indices");
        metrics.put("portfolioAnalysis", portfolioMetrics);
        
        // Exposure Recording metrics
        Map<String, Object> exposureMetrics = new HashMap<>();
        exposureMetrics.put("repositoryAvailable", exposureRepository != null);
        exposureMetrics.put("description", "Classified and protected exposures");
        metrics.put("exposureRecording", exposureMetrics);
        
        // Mitigation metrics
        Map<String, Object> mitigationMetrics = new HashMap<>();
        mitigationMetrics.put("repositoryAvailable", mitigationRepository != null);
        mitigationMetrics.put("description", "Credit risk mitigations");
        metrics.put("mitigation", mitigationMetrics);
        
        // Calculation performance from PerformanceMetrics
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        Map<String, Object> calculationMetrics = new HashMap<>();
        calculationMetrics.put("averageCalculationTimeMs", snapshot.averageProcessingTimeMillis());
        calculationMetrics.put("totalExposuresProcessed", snapshot.totalExposuresProcessed());
        calculationMetrics.put("averageExposuresPerBatch", snapshot.averageExposuresPerBatch());
        calculationMetrics.put("throughputPerHour", snapshot.throughputPerHour());
        metrics.put("calculationPerformance", calculationMetrics);
        
        return metrics;
    }
    
    /**
     * Record for risk calculation metrics data.
     */
    public record RiskCalculationMetrics(
        Instant timestamp,
        Map<String, Object> jvmMetrics,
        Map<String, Object> moduleMetrics,
        Map<String, Object> boundedContextMetrics
    ) {
        public Map<String, Object> toResponseMap() {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("jvm", jvmMetrics);
            metrics.put("performance", moduleMetrics);
            metrics.put("boundedContexts", boundedContextMetrics);
            
            return Map.of(
                "module", "risk-calculation",
                "timestamp", timestamp.toString(),
                "architecture", "bounded-contexts",
                "version", "2.0.0",
                "metrics", metrics
            );
        }
    }
}
