package com.bcbs239.regtech.ingestion.infrastructure.performance;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.ingestion.infrastructure.monitoring.IngestionMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring performance metrics and triggering alerts
 * when thresholds are exceeded.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringService {

    private final IngestionMetricsService metricsService;
    
    // Configurable thresholds
    @Value("${ingestion.performance.processing-time-threshold-ms:30000}")
    private long processingTimeThresholdMs;
    
    @Value("${ingestion.performance.memory-usage-threshold-percent:80}")
    private double memoryUsageThresholdPercent;
    
    @Value("${ingestion.performance.error-rate-threshold-percent:5}")
    private double errorRateThresholdPercent;
    
    @Value("${ingestion.performance.s3-latency-threshold-ms:10000}")
    private long s3LatencyThresholdMs;
    
    @Value("${ingestion.performance.large-file-threshold-mb:100}")
    private long largeFileThresholdMb;
    
    // Performance tracking
    private final Map<String, PerformanceWindow> performanceWindows = new ConcurrentHashMap<>();
    private final AtomicLong lastAlertTime = new AtomicLong(0);
    private final long alertCooldownMs = 300000; // 5 minutes
    
    // Memory monitoring
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    /**
     * Monitor processing time and trigger alerts if threshold exceeded.
     */
    public void monitorProcessingTime(String batchId, String bankId, long processingTimeMs, long fileSizeBytes) {
        // Check if processing time exceeds threshold
        if (processingTimeMs > processingTimeThresholdMs) {
            triggerProcessingTimeAlert(batchId, bankId, processingTimeMs, fileSizeBytes);
        }
        
        // Track performance for trend analysis
        updatePerformanceWindow(bankId, processingTimeMs, fileSizeBytes);
        
        // Log performance metrics
        LoggingConfiguration.logStructured("PROCESSING_TIME_MONITORED", Map.of(
            "batchId", batchId,
            "bankId", bankId,
            "processingTimeMs", processingTimeMs,
            "fileSizeBytes", fileSizeBytes,
            "thresholdExceeded", processingTimeMs > processingTimeThresholdMs,
            "processingRate", calculateProcessingRate(fileSizeBytes, processingTimeMs)
        ));
    }
    
    /**
     * Monitor memory usage during file parsing.
     */
    public void monitorMemoryUsage(String operation, String fileName) {
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        long usedMemory = heapMemory.getUsed();
        long maxMemory = heapMemory.getMax();
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        if (memoryUsagePercent > memoryUsageThresholdPercent) {
            triggerMemoryUsageAlert(operation, fileName, memoryUsagePercent, usedMemory, maxMemory);
        }
        
        LoggingConfiguration.logStructured("MEMORY_USAGE_MONITORED", Map.of(
            "operation", operation,
            "fileName", fileName,
            "usedMemoryMB", usedMemory / 1024 / 1024,
            "maxMemoryMB", maxMemory / 1024 / 1024,
            "memoryUsagePercent", String.format("%.1f", memoryUsagePercent),
            "thresholdExceeded", memoryUsagePercent > memoryUsageThresholdPercent
        ));
    }
    
    /**
     * Monitor system performance under high load.
     */
    public void monitorSystemLoad(int activeProcessingCount, int queueSize) {
        Runtime runtime = Runtime.getRuntime();
        int availableProcessors = runtime.availableProcessors();
        
        // Calculate load metrics
        double processingLoad = (double) activeProcessingCount / availableProcessors;
        boolean highLoad = processingLoad > 0.8 || queueSize > 10;
        
        if (highLoad) {
            triggerHighLoadAlert(activeProcessingCount, queueSize, availableProcessors, processingLoad);
        }
        
        LoggingConfiguration.logStructured("SYSTEM_LOAD_MONITORED", Map.of(
            "activeProcessingCount", activeProcessingCount,
            "queueSize", queueSize,
            "availableProcessors", availableProcessors,
            "processingLoad", String.format("%.2f", processingLoad),
            "highLoad", highLoad
        ));
    }
    
    /**
     * Monitor error rates and trigger alerts.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorErrorRates() {
        double fileProcessingSuccessRate = metricsService.getFileProcessingSuccessRate();
        double s3UploadSuccessRate = metricsService.getS3UploadSuccessRate();
        
        double fileErrorRate = (1.0 - fileProcessingSuccessRate) * 100;
        double s3ErrorRate = (1.0 - s3UploadSuccessRate) * 100;
        
        if (fileErrorRate > errorRateThresholdPercent) {
            triggerErrorRateAlert("FILE_PROCESSING", fileErrorRate);
        }
        
        if (s3ErrorRate > errorRateThresholdPercent) {
            triggerErrorRateAlert("S3_OPERATIONS", s3ErrorRate);
        }
        
        LoggingConfiguration.logStructured("ERROR_RATES_MONITORED", Map.of(
            "fileProcessingSuccessRate", String.format("%.2f", fileProcessingSuccessRate * 100),
            "s3UploadSuccessRate", String.format("%.2f", s3UploadSuccessRate * 100),
            "fileErrorRate", String.format("%.2f", fileErrorRate),
            "s3ErrorRate", String.format("%.2f", s3ErrorRate),
            "fileErrorThresholdExceeded", fileErrorRate > errorRateThresholdPercent,
            "s3ErrorThresholdExceeded", s3ErrorRate > errorRateThresholdPercent
        ));
    }
    
    /**
     * Monitor S3 operation latencies.
     */
    public void monitorS3Latency(String operation, String bankId, long latencyMs, long fileSizeBytes) {
        if (latencyMs > s3LatencyThresholdMs) {
            triggerS3LatencyAlert(operation, bankId, latencyMs, fileSizeBytes);
        }
        
        LoggingConfiguration.logStructured("S3_LATENCY_MONITORED", Map.of(
            "operation", operation,
            "bankId", bankId,
            "latencyMs", latencyMs,
            "fileSizeBytes", fileSizeBytes,
            "thresholdExceeded", latencyMs > s3LatencyThresholdMs,
            "throughputMBps", calculateThroughput(fileSizeBytes, latencyMs)
        ));
    }
    
    /**
     * Generate performance report with current metrics.
     */
    public PerformanceReport generatePerformanceReport() {
        Map<String, Object> metricsSnapshot = metricsService.getPerformanceSnapshot();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        
        return PerformanceReport.builder()
            .timestamp(Instant.now())
            .metricsSnapshot(metricsSnapshot)
            .memoryUsageMB(heapMemory.getUsed() / 1024 / 1024)
            .maxMemoryMB(heapMemory.getMax() / 1024 / 1024)
            .memoryUsagePercent((double) heapMemory.getUsed() / heapMemory.getMax() * 100)
            .performanceWindows(Map.copyOf(performanceWindows))
            .thresholds(Map.of(
                "processingTimeThresholdMs", processingTimeThresholdMs,
                "memoryUsageThresholdPercent", memoryUsageThresholdPercent,
                "errorRateThresholdPercent", errorRateThresholdPercent,
                "s3LatencyThresholdMs", s3LatencyThresholdMs
            ))
            .build();
    }
    
    private void triggerProcessingTimeAlert(String batchId, String bankId, long processingTimeMs, long fileSizeBytes) {
        if (shouldTriggerAlert()) {
            log.warn("PERFORMANCE_ALERT: Processing time threshold exceeded for batch {} (bank: {}). " +
                    "Processing time: {}ms, threshold: {}ms, file size: {}MB",
                batchId, bankId, processingTimeMs, processingTimeThresholdMs, fileSizeBytes / 1024 / 1024);
            
            LoggingConfiguration.logStructured("PROCESSING_TIME_ALERT", Map.of(
                "alertType", "PROCESSING_TIME_THRESHOLD_EXCEEDED",
                "batchId", batchId,
                "bankId", bankId,
                "processingTimeMs", processingTimeMs,
                "thresholdMs", processingTimeThresholdMs,
                "fileSizeMB", fileSizeBytes / 1024 / 1024,
                "severity", "WARNING"
            ));
        }
    }
    
    private void triggerMemoryUsageAlert(String operation, String fileName, double memoryUsagePercent, 
                                       long usedMemory, long maxMemory) {
        if (shouldTriggerAlert()) {
            log.warn("PERFORMANCE_ALERT: Memory usage threshold exceeded during {} for file {}. " +
                    "Memory usage: {:.1f}%, threshold: {:.1f}%, used: {}MB, max: {}MB",
                operation, fileName, memoryUsagePercent, memoryUsageThresholdPercent,
                usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
            
            LoggingConfiguration.logStructured("MEMORY_USAGE_ALERT", Map.of(
                "alertType", "MEMORY_USAGE_THRESHOLD_EXCEEDED",
                "operation", operation,
                "fileName", fileName,
                "memoryUsagePercent", String.format("%.1f", memoryUsagePercent),
                "thresholdPercent", memoryUsageThresholdPercent,
                "usedMemoryMB", usedMemory / 1024 / 1024,
                "maxMemoryMB", maxMemory / 1024 / 1024,
                "severity", "WARNING"
            ));
        }
    }
    
    private void triggerHighLoadAlert(int activeProcessingCount, int queueSize, 
                                    int availableProcessors, double processingLoad) {
        if (shouldTriggerAlert()) {
            log.warn("PERFORMANCE_ALERT: High system load detected. " +
                    "Active processing: {}, queue size: {}, processors: {}, load: {:.2f}",
                activeProcessingCount, queueSize, availableProcessors, processingLoad);
            
            LoggingConfiguration.logStructured("HIGH_LOAD_ALERT", Map.of(
                "alertType", "HIGH_SYSTEM_LOAD",
                "activeProcessingCount", activeProcessingCount,
                "queueSize", queueSize,
                "availableProcessors", availableProcessors,
                "processingLoad", String.format("%.2f", processingLoad),
                "severity", "WARNING"
            ));
        }
    }
    
    private void triggerErrorRateAlert(String component, double errorRate) {
        if (shouldTriggerAlert()) {
            log.warn("PERFORMANCE_ALERT: Error rate threshold exceeded for {}. " +
                    "Error rate: {:.2f}%, threshold: {:.2f}%",
                component, errorRate, errorRateThresholdPercent);
            
            LoggingConfiguration.logStructured("ERROR_RATE_ALERT", Map.of(
                "alertType", "ERROR_RATE_THRESHOLD_EXCEEDED",
                "component", component,
                "errorRate", String.format("%.2f", errorRate),
                "thresholdPercent", errorRateThresholdPercent,
                "severity", "CRITICAL"
            ));
        }
    }
    
    private void triggerS3LatencyAlert(String operation, String bankId, long latencyMs, long fileSizeBytes) {
        if (shouldTriggerAlert()) {
            log.warn("PERFORMANCE_ALERT: S3 latency threshold exceeded for {} operation (bank: {}). " +
                    "Latency: {}ms, threshold: {}ms, file size: {}MB",
                operation, bankId, latencyMs, s3LatencyThresholdMs, fileSizeBytes / 1024 / 1024);
            
            LoggingConfiguration.logStructured("S3_LATENCY_ALERT", Map.of(
                "alertType", "S3_LATENCY_THRESHOLD_EXCEEDED",
                "operation", operation,
                "bankId", bankId,
                "latencyMs", latencyMs,
                "thresholdMs", s3LatencyThresholdMs,
                "fileSizeMB", fileSizeBytes / 1024 / 1024,
                "severity", "WARNING"
            ));
        }
    }
    
    private boolean shouldTriggerAlert() {
        long currentTime = System.currentTimeMillis();
        long lastAlert = lastAlertTime.get();
        
        if (currentTime - lastAlert > alertCooldownMs) {
            lastAlertTime.set(currentTime);
            return true;
        }
        
        return false;
    }
    
    private void updatePerformanceWindow(String bankId, long processingTimeMs, long fileSizeBytes) {
        performanceWindows.computeIfAbsent(bankId, k -> new PerformanceWindow())
            .addDataPoint(processingTimeMs, fileSizeBytes);
    }
    
    private double calculateProcessingRate(long fileSizeBytes, long processingTimeMs) {
        if (processingTimeMs == 0) return 0;
        return (fileSizeBytes / 1024.0 / 1024.0) / (processingTimeMs / 1000.0); // MB/s
    }
    
    private double calculateThroughput(long fileSizeBytes, long latencyMs) {
        if (latencyMs == 0) return 0;
        return (fileSizeBytes / 1024.0 / 1024.0) / (latencyMs / 1000.0); // MB/s
    }
    
    /**
     * Performance window for tracking trends.
     */
    public static class PerformanceWindow {
        private final AtomicLong totalProcessingTime = new AtomicLong(0);
        private final AtomicLong totalFileSize = new AtomicLong(0);
        private final AtomicLong sampleCount = new AtomicLong(0);
        private volatile long maxProcessingTime = 0;
        private volatile long minProcessingTime = Long.MAX_VALUE;
        
        public void addDataPoint(long processingTimeMs, long fileSizeBytes) {
            totalProcessingTime.addAndGet(processingTimeMs);
            totalFileSize.addAndGet(fileSizeBytes);
            sampleCount.incrementAndGet();
            
            synchronized (this) {
                maxProcessingTime = Math.max(maxProcessingTime, processingTimeMs);
                minProcessingTime = Math.min(minProcessingTime, processingTimeMs);
            }
        }
        
        public double getAverageProcessingTime() {
            long count = sampleCount.get();
            return count > 0 ? (double) totalProcessingTime.get() / count : 0;
        }
        
        public double getAverageFileSize() {
            long count = sampleCount.get();
            return count > 0 ? (double) totalFileSize.get() / count : 0;
        }
        
        public long getMaxProcessingTime() { return maxProcessingTime; }
        public long getMinProcessingTime() { return minProcessingTime == Long.MAX_VALUE ? 0 : minProcessingTime; }
        public long getSampleCount() { return sampleCount.get(); }
    }
    
    /**
     * Performance report data structure.
     */
    public static class PerformanceReport {
        private final Instant timestamp;
        private final Map<String, Object> metricsSnapshot;
        private final long memoryUsageMB;
        private final long maxMemoryMB;
        private final double memoryUsagePercent;
        private final Map<String, PerformanceWindow> performanceWindows;
        private final Map<String, Object> thresholds;
        
        private PerformanceReport(Builder builder) {
            this.timestamp = builder.timestamp;
            this.metricsSnapshot = builder.metricsSnapshot;
            this.memoryUsageMB = builder.memoryUsageMB;
            this.maxMemoryMB = builder.maxMemoryMB;
            this.memoryUsagePercent = builder.memoryUsagePercent;
            this.performanceWindows = builder.performanceWindows;
            this.thresholds = builder.thresholds;
        }
        
        public static Builder builder() { return new Builder(); }
        
        // Getters
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getMetricsSnapshot() { return metricsSnapshot; }
        public long getMemoryUsageMB() { return memoryUsageMB; }
        public long getMaxMemoryMB() { return maxMemoryMB; }
        public double getMemoryUsagePercent() { return memoryUsagePercent; }
        public Map<String, PerformanceWindow> getPerformanceWindows() { return performanceWindows; }
        public Map<String, Object> getThresholds() { return thresholds; }
        
        public static class Builder {
            private Instant timestamp;
            private Map<String, Object> metricsSnapshot;
            private long memoryUsageMB;
            private long maxMemoryMB;
            private double memoryUsagePercent;
            private Map<String, PerformanceWindow> performanceWindows;
            private Map<String, Object> thresholds;
            
            public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
            public Builder metricsSnapshot(Map<String, Object> metricsSnapshot) { this.metricsSnapshot = metricsSnapshot; return this; }
            public Builder memoryUsageMB(long memoryUsageMB) { this.memoryUsageMB = memoryUsageMB; return this; }
            public Builder maxMemoryMB(long maxMemoryMB) { this.maxMemoryMB = maxMemoryMB; return this; }
            public Builder memoryUsagePercent(double memoryUsagePercent) { this.memoryUsagePercent = memoryUsagePercent; return this; }
            public Builder performanceWindows(Map<String, PerformanceWindow> performanceWindows) { this.performanceWindows = performanceWindows; return this; }
            public Builder thresholds(Map<String, Object> thresholds) { this.thresholds = thresholds; return this; }
            
            public PerformanceReport build() { return new PerformanceReport(this); }
        }
    }
}