package com.bcbs239.regtech.riskcalculation.application.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for performance monitoring and maintenance.
 * Periodically logs performance dashboards and cleans up old metrics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringScheduler {
    
    private final PerformanceMetrics performanceMetrics;
    
    /**
     * Logs performance dashboard every 5 minutes.
     * Provides visibility into system performance and throughput.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logPerformanceDashboard() {
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        
        log.info("=== PERFORMANCE DASHBOARD ===");
        log.info("Total Batches Processed: {}", snapshot.totalBatchesProcessed());
        log.info("Total Batches Failed: {}", snapshot.totalBatchesFailed());
        log.info("Total Exposures Processed: {}", snapshot.totalExposuresProcessed());
        log.info("Average Processing Time: {:.2f}ms", snapshot.averageProcessingTimeMillis());
        log.info("Error Rate: {:.2f}%", snapshot.errorRatePercent());
        log.info("Active Calculations: {}", snapshot.activeCalculations());
        log.info("Throughput: {:.2f} batches/hour", snapshot.throughputPerHour());
        log.info("Average Exposures per Batch: {:.2f}", snapshot.averageExposuresPerBatch());
        log.info("Timestamp: {}", snapshot.timestamp());
        log.info("============================");
    }
    
    /**
     * Cleans up old batch processing times every hour.
     * Keeps only the last 1000 batch times to prevent memory leaks.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupOldMetrics() {
        performanceMetrics.cleanupOldBatchTimes(1000);
        log.debug("Cleaned up old batch processing times");
    }
    
    /**
     * Resets throughput tracking window every 24 hours.
     * Provides daily throughput statistics.
     */
    @Scheduled(cron = "0 0 0 * * *") // Midnight every day
    public void resetThroughputWindow() {
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        
        log.info("=== DAILY THROUGHPUT REPORT ===");
        log.info("Batches Processed (24h): {}", snapshot.totalBatchesProcessed());
        log.info("Throughput: {:.2f} batches/hour", snapshot.throughputPerHour());
        log.info("===============================");
        
        performanceMetrics.resetThroughputWindow();
    }
}
