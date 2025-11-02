package com.bcbs239.regtech.ingestion.infrastructure.performance;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.HashMap;

/**
 * Configuration for performance monitoring thresholds and alerting rules.
 */
@Configuration
@ConfigurationProperties(prefix = "ingestion.performance")
@Data
public class PerformanceAlertingConfiguration {

    /**
     * Processing time thresholds by file size categories.
     */
    private ProcessingTimeThresholds processingTime = new ProcessingTimeThresholds();
    
    /**
     * Memory usage monitoring configuration.
     */
    private MemoryMonitoring memory = new MemoryMonitoring();
    
    /**
     * Error rate thresholds for different components.
     */
    private ErrorRateThresholds errorRates = new ErrorRateThresholds();
    
    /**
     * S3 operation performance thresholds.
     */
    private S3PerformanceThresholds s3Performance = new S3PerformanceThresholds();
    
    /**
     * System load monitoring configuration.
     */
    private SystemLoadMonitoring systemLoad = new SystemLoadMonitoring();
    
    /**
     * Alert configuration settings.
     */
    private AlertConfiguration alerts = new AlertConfiguration();

    @Data
    public static class ProcessingTimeThresholds {
        /**
         * Threshold for small files (< 10MB) in milliseconds.
         */
        private long smallFileThresholdMs = 5000;
        
        /**
         * Threshold for medium files (10MB - 100MB) in milliseconds.
         */
        private long mediumFileThresholdMs = 30000;
        
        /**
         * Threshold for large files (> 100MB) in milliseconds.
         */
        private long largeFileThresholdMs = 120000;
        
        /**
         * File size boundaries in bytes.
         */
        private long smallFileBoundary = 10 * 1024 * 1024; // 10MB
        private long mediumFileBoundary = 100 * 1024 * 1024; // 100MB
        
        /**
         * Get appropriate threshold based on file size.
         */
        public long getThresholdForFileSize(long fileSizeBytes) {
            if (fileSizeBytes < smallFileBoundary) {
                return smallFileThresholdMs;
            } else if (fileSizeBytes < mediumFileBoundary) {
                return mediumFileThresholdMs;
            } else {
                return largeFileThresholdMs;
            }
        }
    }

    @Data
    public static class MemoryMonitoring {
        /**
         * Memory usage threshold percentage to trigger alerts.
         */
        private double usageThresholdPercent = 80.0;
        
        /**
         * Critical memory usage threshold percentage.
         */
        private double criticalThresholdPercent = 90.0;
        
        /**
         * Memory monitoring interval in milliseconds.
         */
        private long monitoringIntervalMs = 30000;
        
        /**
         * Enable garbage collection suggestions when memory is high.
         */
        private boolean enableGcSuggestions = true;
    }

    @Data
    public static class ErrorRateThresholds {
        /**
         * File processing error rate threshold percentage.
         */
        private double fileProcessingThresholdPercent = 5.0;
        
        /**
         * S3 operation error rate threshold percentage.
         */
        private double s3OperationThresholdPercent = 2.0;
        
        /**
         * Database operation error rate threshold percentage.
         */
        private double databaseOperationThresholdPercent = 1.0;
        
        /**
         * Bank Registry service error rate threshold percentage.
         */
        private double bankRegistryThresholdPercent = 10.0;
        
        /**
         * Time window for error rate calculation in minutes.
         */
        private int errorRateWindowMinutes = 5;
    }

    @Data
    public static class S3PerformanceThresholds {
        /**
         * S3 upload latency threshold in milliseconds.
         */
        private long uploadLatencyThresholdMs = 10000;
        
        /**
         * S3 download latency threshold in milliseconds.
         */
        private long downloadLatencyThresholdMs = 5000;
        
        /**
         * Minimum throughput threshold in MB/s.
         */
        private double minThroughputMBps = 1.0;
        
        /**
         * Multipart upload threshold in bytes.
         */
        private long multipartThreshold = 100 * 1024 * 1024; // 100MB
        
        /**
         * S3 operation timeout in milliseconds.
         */
        private long operationTimeoutMs = 300000; // 5 minutes
    }

    @Data
    public static class SystemLoadMonitoring {
        /**
         * CPU usage threshold percentage.
         */
        private double cpuUsageThresholdPercent = 80.0;
        
        /**
         * Maximum concurrent processing threshold.
         */
        private int maxConcurrentProcessingThreshold = 8;
        
        /**
         * Queue size threshold for processing backlog.
         */
        private int queueSizeThreshold = 10;
        
        /**
         * System load monitoring interval in milliseconds.
         */
        private long monitoringIntervalMs = 60000;
    }

    @Data
    public static class AlertConfiguration {
        /**
         * Alert cooldown period in milliseconds to prevent spam.
         */
        private long cooldownMs = 300000; // 5 minutes
        
        /**
         * Enable email alerts (requires email configuration).
         */
        private boolean enableEmailAlerts = false;
        
        /**
         * Enable Slack alerts (requires Slack configuration).
         */
        private boolean enableSlackAlerts = false;
        
        /**
         * Enable metrics-based alerts (Prometheus/Grafana).
         */
        private boolean enableMetricsAlerts = true;
        
        /**
         * Alert severity levels configuration.
         */
        private Map<String, String> severityLevels = new HashMap<>() {{
            put("PROCESSING_TIME_THRESHOLD_EXCEEDED", "WARNING");
            put("MEMORY_USAGE_THRESHOLD_EXCEEDED", "WARNING");
            put("HIGH_SYSTEM_LOAD", "WARNING");
            put("ERROR_RATE_THRESHOLD_EXCEEDED", "CRITICAL");
            put("S3_LATENCY_THRESHOLD_EXCEEDED", "WARNING");
            put("CRITICAL_MEMORY_USAGE", "CRITICAL");
            put("SYSTEM_OVERLOAD", "CRITICAL");
        }};
        
        /**
         * Recipients for different alert types.
         */
        private Map<String, String[]> alertRecipients = new HashMap<>() {{
            put("WARNING", new String[]{"devops-team@company.com"});
            put("CRITICAL", new String[]{"devops-team@company.com", "engineering-lead@company.com"});
        }};
    }

    /**
     * Get processing time threshold based on file size and type.
     */
    public long getProcessingTimeThreshold(long fileSizeBytes, String fileType) {
        long baseThreshold = processingTime.getThresholdForFileSize(fileSizeBytes);
        
        // Adjust threshold based on file type
        if ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(fileType)) {
            // Excel files typically take longer to process
            return (long) (baseThreshold * 1.5);
        }
        
        return baseThreshold;
    }

    /**
     * Check if alert should be triggered based on severity and configuration.
     */
    public boolean shouldTriggerAlert(String alertType) {
        String severity = alerts.getSeverityLevels().getOrDefault(alertType, "INFO");
        
        // Always trigger CRITICAL alerts
        if ("CRITICAL".equals(severity)) {
            return true;
        }
        
        // Trigger WARNING alerts if metrics alerts are enabled
        if ("WARNING".equals(severity)) {
            return alerts.isEnableMetricsAlerts();
        }
        
        return false;
    }

    /**
     * Get alert recipients for a given severity level.
     */
    public String[] getAlertRecipients(String alertType) {
        String severity = alerts.getSeverityLevels().getOrDefault(alertType, "INFO");
        return alerts.getAlertRecipients().getOrDefault(severity, new String[0]);
    }

    /**
     * Validate configuration on startup.
     */
    public void validateConfiguration() {
        if (processingTime.getSmallFileThresholdMs() <= 0) {
            throw new IllegalArgumentException("Small file processing threshold must be positive");
        }
        
        if (memory.getUsageThresholdPercent() <= 0 || memory.getUsageThresholdPercent() > 100) {
            throw new IllegalArgumentException("Memory usage threshold must be between 0 and 100");
        }
        
        if (errorRates.getFileProcessingThresholdPercent() < 0 || errorRates.getFileProcessingThresholdPercent() > 100) {
            throw new IllegalArgumentException("Error rate threshold must be between 0 and 100");
        }
        
        if (alerts.getCooldownMs() < 0) {
            throw new IllegalArgumentException("Alert cooldown must be non-negative");
        }
    }
}