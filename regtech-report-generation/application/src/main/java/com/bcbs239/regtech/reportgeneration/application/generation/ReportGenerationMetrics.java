package com.bcbs239.regtech.reportgeneration.application.generation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks performance metrics for report generation operations.
 * Provides thread-safe counters and timing information for monitoring.
 * 
 * Requirements: 16.1, 16.2, 16.3, 19.1, 19.2, 19.3
 */
@Component
@Slf4j
public class ReportGenerationMetrics {
    
    // Counters for report generation
    private final LongAdder totalReportsGenerated = new LongAdder();
    private final LongAdder totalReportsFailed = new LongAdder();
    private final LongAdder totalReportsPartial = new LongAdder();
    private final LongAdder totalDuplicatesSkipped = new LongAdder();
    
    // Timing metrics
    private final ConcurrentHashMap<String, Long> reportStartTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalGenerationTimeMillis = new AtomicLong(0);
    private final AtomicLong totalDataFetchTimeMillis = new AtomicLong(0);
    private final AtomicLong totalHtmlGenerationTimeMillis = new AtomicLong(0);
    private final AtomicLong totalXbrlGenerationTimeMillis = new AtomicLong(0);
    
    // Active generations tracking
    private final LongAdder activeGenerations = new LongAdder();
    
    // Failure reasons tracking
    private final ConcurrentHashMap<String, LongAdder> failureReasons = new ConcurrentHashMap<>();
    
    /**
     * Records the start of a report generation.
     */
    public void recordReportGenerationStart(String batchId) {
        reportStartTimes.put(batchId, System.currentTimeMillis());
        activeGenerations.increment();
        
        log.debug("Report generation started [batchId:{},activeGenerations:{}]",
            batchId, activeGenerations.sum());
    }
    
    /**
     * Records successful completion of report generation.
     */
    public void recordReportGenerationSuccess(String batchId, long durationMillis) {
        reportStartTimes.remove(batchId);
        
        totalReportsGenerated.increment();
        totalGenerationTimeMillis.addAndGet(durationMillis);
        activeGenerations.decrement();
        
        log.info("Report generation completed [batchId:{},duration:{}ms,activeGenerations:{}]",
            batchId, durationMillis, activeGenerations.sum());
    }
    
    /**
     * Records partial report generation (one format failed).
     */
    public void recordReportGenerationPartial(String batchId, String reason) {
        reportStartTimes.remove(batchId);
        
        totalReportsPartial.increment();
        activeGenerations.decrement();
        
        failureReasons.computeIfAbsent(reason, k -> new LongAdder()).increment();
        
        log.warn("Report generation partial [batchId:{},reason:{},activeGenerations:{}]",
            batchId, reason, activeGenerations.sum());
    }
    
    /**
     * Records failed report generation.
     */
    public void recordReportGenerationFailure(String batchId, String reason, long durationMillis) {
        reportStartTimes.remove(batchId);
        
        totalReportsFailed.increment();
        activeGenerations.decrement();
        
        failureReasons.computeIfAbsent(reason, k -> new LongAdder()).increment();
        
        log.warn("Report generation failed [batchId:{},duration:{}ms,reason:{},activeGenerations:{}]",
            batchId, durationMillis, reason, activeGenerations.sum());
    }
    
    /**
     * Records a duplicate report generation attempt that was skipped.
     */
    public void recordDuplicateSkipped(String batchId) {
        totalDuplicatesSkipped.increment();
        
        log.debug("Duplicate report generation skipped [batchId:{},totalSkipped:{}]",
            batchId, totalDuplicatesSkipped.sum());
    }
    
    /**
     * Records data fetch duration.
     */
    public void recordDataFetchDuration(String batchId, long durationMillis) {
        totalDataFetchTimeMillis.addAndGet(durationMillis);
        
        log.debug("Data fetch completed [batchId:{},duration:{}ms]", batchId, durationMillis);
    }
    
    /**
     * Records HTML generation duration.
     */
    public void recordHtmlGenerationDuration(String batchId, long durationMillis) {
        totalHtmlGenerationTimeMillis.addAndGet(durationMillis);
        
        log.debug("HTML generation completed [batchId:{},duration:{}ms]", batchId, durationMillis);
    }
    
    /**
     * Records XBRL generation duration.
     */
    public void recordXbrlGenerationDuration(String batchId, long durationMillis) {
        totalXbrlGenerationTimeMillis.addAndGet(durationMillis);
        
        log.debug("XBRL generation completed [batchId:{},duration:{}ms]", batchId, durationMillis);
    }
    
    /**
     * Gets the current metrics snapshot.
     */
    public MetricsSnapshot getSnapshot() {
        long totalReports = totalReportsGenerated.sum();
        long totalFailed = totalReportsFailed.sum();
        long totalPartial = totalReportsPartial.sum();
        long totalSkipped = totalDuplicatesSkipped.sum();
        
        long totalTime = totalGenerationTimeMillis.get();
        long totalDataFetch = totalDataFetchTimeMillis.get();
        long totalHtml = totalHtmlGenerationTimeMillis.get();
        long totalXbrl = totalXbrlGenerationTimeMillis.get();
        
        double averageGenerationTime = totalReports > 0 ? (double) totalTime / totalReports : 0.0;
        double averageDataFetchTime = totalReports > 0 ? (double) totalDataFetch / totalReports : 0.0;
        double averageHtmlTime = totalReports > 0 ? (double) totalHtml / totalReports : 0.0;
        double averageXbrlTime = totalReports > 0 ? (double) totalXbrl / totalReports : 0.0;
        
        double failureRate = (totalReports + totalFailed) > 0 
            ? (double) totalFailed / (totalReports + totalFailed) * 100.0 
            : 0.0;
        
        double partialRate = (totalReports + totalPartial) > 0
            ? (double) totalPartial / (totalReports + totalPartial) * 100.0
            : 0.0;
        
        // Convert failure reasons map to regular map
        Map<String, Long> failureReasonsSnapshot = new ConcurrentHashMap<>();
        failureReasons.forEach((reason, counter) -> 
            failureReasonsSnapshot.put(reason, counter.sum()));
        
        return new MetricsSnapshot(
            totalReports,
            totalFailed,
            totalPartial,
            totalSkipped,
            averageGenerationTime,
            averageDataFetchTime,
            averageHtmlTime,
            averageXbrlTime,
            failureRate,
            partialRate,
            activeGenerations.sum(),
            failureReasonsSnapshot,
            Instant.now()
        );
    }
    
    /**
     * Clears old report start times to prevent memory leaks.
     */
    public void cleanupOldReportTimes(int keepLastN) {
        if (reportStartTimes.size() > keepLastN) {
            reportStartTimes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .skip(keepLastN)
                .map(Map.Entry::getKey)
                .forEach(reportStartTimes::remove);
            
            log.debug("Cleaned up old report start times [kept:{},removed:{}]",
                keepLastN, reportStartTimes.size() - keepLastN);
        }
    }
    
    /**
     * Snapshot of current metrics.
     */
    public record MetricsSnapshot(
        long totalReportsGenerated,
        long totalReportsFailed,
        long totalReportsPartial,
        long totalDuplicatesSkipped,
        double averageGenerationTimeMillis,
        double averageDataFetchTimeMillis,
        double averageHtmlGenerationTimeMillis,
        double averageXbrlGenerationTimeMillis,
        double failureRatePercent,
        double partialRatePercent,
        long activeGenerations,
        Map<String, Long> failureReasons,
        Instant timestamp
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("totalReportsGenerated", totalReportsGenerated);
            map.put("totalReportsFailed", totalReportsFailed);
            map.put("totalReportsPartial", totalReportsPartial);
            map.put("totalDuplicatesSkipped", totalDuplicatesSkipped);
            map.put("averageGenerationTimeMillis", averageGenerationTimeMillis);
            map.put("averageDataFetchTimeMillis", averageDataFetchTimeMillis);
            map.put("averageHtmlGenerationTimeMillis", averageHtmlGenerationTimeMillis);
            map.put("averageXbrlGenerationTimeMillis", averageXbrlGenerationTimeMillis);
            map.put("failureRatePercent", failureRatePercent);
            map.put("partialRatePercent", partialRatePercent);
            map.put("activeGenerations", activeGenerations);
            map.put("failureReasons", failureReasons);
            map.put("timestamp", timestamp.toString());
            return map;
        }
    }
}
