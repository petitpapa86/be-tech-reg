package com.bcbs239.regtech.riskcalculation.infrastructure.monitoring;

import com.bcbs239.regtech.riskcalculation.domain.shared.IPerformanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks performance metrics for risk calculation operations.
 * Provides thread-safe counters and timing information for monitoring.
 */
@Component
@Slf4j
public class PerformanceMetrics implements IPerformanceMetrics {
    
    // Counters for batch processing
    private final LongAdder totalBatchesProcessed = new LongAdder();
    private final LongAdder totalBatchesFailed = new LongAdder();
    private final LongAdder totalExposuresProcessed = new LongAdder();
    
    // Timing metrics
    private final ConcurrentHashMap<String, Long> batchStartTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> batchProcessingTimes = new ConcurrentHashMap<>();
    private final AtomicLong totalProcessingTimeMillis = new AtomicLong(0);
    
    // Throughput tracking
    private volatile Instant lastResetTime = Instant.now();
    private final LongAdder batchesSinceLastReset = new LongAdder();
    
    // Active calculations tracking
    private final LongAdder activeCalculations = new LongAdder();
    
    /**
     * Records the start of a batch calculation.
     */
    @Override
    public void recordBatchStart(String batchId) {
        batchStartTimes.put(batchId, System.currentTimeMillis());
        activeCalculations.increment();
        
        log.debug("Batch calculation started [batchId:{},activeCalculations:{}]", 
            batchId, activeCalculations.sum());
    }
    
    /**
     * Records the successful completion of a batch calculation.
     */
    @Override
    public void recordBatchSuccess(String batchId, int exposureCount) {
        Long startTime = batchStartTimes.remove(batchId);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            batchProcessingTimes.put(batchId, duration);
            totalProcessingTimeMillis.addAndGet(duration);
            
            totalBatchesProcessed.increment();
            totalExposuresProcessed.add(exposureCount);
            batchesSinceLastReset.increment();
            activeCalculations.decrement();
            
            // Calculate throughput
            double throughput = exposureCount * 1000.0 / duration;
            
            log.info("Batch calculation completed [batchId:{},duration:{}ms,exposures:{},throughput:{}/sec,activeCalculations:{}]",
                batchId, duration, exposureCount, String.format("%.2f", throughput), activeCalculations.sum());
        }
    }
    
    /**
     * Records a failed batch calculation.
     */
    @Override
    public void recordBatchFailure(String batchId, String errorMessage) {
        Long startTime = batchStartTimes.remove(batchId);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            
            totalBatchesFailed.increment();
            activeCalculations.decrement();
            
            log.warn("Batch calculation failed [batchId:{},duration:{}ms,error:{},activeCalculations:{}]",
                batchId, duration, errorMessage, activeCalculations.sum());
        }
    }
    
    /**
     * Gets the current metrics snapshot.
     */
    @Override
    public MetricsSnapshot getSnapshot() {
        long totalBatches = totalBatchesProcessed.sum();
        long totalFailed = totalBatchesFailed.sum();
        long totalExposures = totalExposuresProcessed.sum();
        long totalTime = totalProcessingTimeMillis.get();
        
        double averageProcessingTime = totalBatches > 0 ? (double) totalTime / totalBatches : 0.0;
        long totalAttempts = totalBatches + totalFailed;
        double errorRate = totalAttempts > 0 ? (double) totalFailed / totalAttempts * 100.0 : 0.0;
        double averageExposuresPerBatch = totalBatches > 0 ? (double) totalExposures / totalBatches : 0.0;
        
        // Calculate throughput per hour
        Duration timeSinceReset = Duration.between(lastResetTime, Instant.now());
        double hoursSinceReset = timeSinceReset.toMillis() / (1000.0 * 60.0 * 60.0);
        double throughputPerHour = hoursSinceReset > 0 ? batchesSinceLastReset.sum() / hoursSinceReset : 0.0;
        
        return new MetricsSnapshot(
            totalBatches,
            totalFailed,
            totalExposures,
            averageProcessingTime,
            errorRate,
            activeCalculations.sum(),
            throughputPerHour,
            averageExposuresPerBatch,
            Instant.now()
        );
    }
    
    /**
     * Resets the throughput tracking window.
     */
    @Override
    public void resetThroughputWindow() {
        lastResetTime = Instant.now();
        batchesSinceLastReset.reset();
        log.info("Throughput tracking window reset");
    }
    
    /**
     * Gets the processing time for a specific batch.
     */
    @Override
    public Long getBatchProcessingTime(String batchId) {
        return batchProcessingTimes.get(batchId);
    }
    
    /**
     * Clears old batch processing times to prevent memory leaks.
     * Should be called periodically to clean up completed batches.
     */
    @Override
    public void cleanupOldBatchTimes(int keepLastN) {
        if (batchProcessingTimes.size() > keepLastN) {
            // Keep only the most recent N entries
            batchProcessingTimes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .skip(keepLastN)
                .map(Map.Entry::getKey)
                .forEach(batchProcessingTimes::remove);
            
            log.debug("Cleaned up old batch processing times [kept:{},removed:{}]", 
                keepLastN, batchProcessingTimes.size() - keepLastN);
        }
    }

}
