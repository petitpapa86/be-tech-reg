package com.bcbs239.regtech.riskcalculation.application.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PerformanceMetrics tracking.
 * Verifies that metrics are correctly recorded and calculated.
 */
class PerformanceMetricsTest {
    
    private PerformanceMetrics performanceMetrics;
    
    @BeforeEach
    void setUp() {
        performanceMetrics = new PerformanceMetrics();
    }
    
    @Test
    void testRecordBatchSuccess() {
        // Record a successful batch
        performanceMetrics.recordBatchStart("batch-1");
        
        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        performanceMetrics.recordBatchSuccess("batch-1", 100);
        
        // Verify metrics
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        assertEquals(1, snapshot.totalBatchesProcessed());
        assertEquals(0, snapshot.totalBatchesFailed());
        assertEquals(100, snapshot.totalExposuresProcessed());
        assertTrue(snapshot.averageProcessingTimeMillis() >= 100);
        assertEquals(0.0, snapshot.errorRatePercent());
    }
    
    @Test
    void testRecordBatchFailure() {
        // Record a failed batch
        performanceMetrics.recordBatchStart("batch-1");
        performanceMetrics.recordBatchFailure("batch-1", "Test error");
        
        // Verify metrics
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        assertEquals(0, snapshot.totalBatchesProcessed());
        assertEquals(1, snapshot.totalBatchesFailed());
        assertEquals(100.0, snapshot.errorRatePercent());
    }
    
    @Test
    void testActiveCalculationsTracking() {
        // Start multiple batches
        performanceMetrics.recordBatchStart("batch-1");
        performanceMetrics.recordBatchStart("batch-2");
        performanceMetrics.recordBatchStart("batch-3");
        
        PerformanceMetrics.MetricsSnapshot snapshot1 = performanceMetrics.getSnapshot();
        assertEquals(3, snapshot1.activeCalculations());
        
        // Complete one batch
        performanceMetrics.recordBatchSuccess("batch-1", 50);
        
        PerformanceMetrics.MetricsSnapshot snapshot2 = performanceMetrics.getSnapshot();
        assertEquals(2, snapshot2.activeCalculations());
        
        // Fail one batch
        performanceMetrics.recordBatchFailure("batch-2", "Error");
        
        PerformanceMetrics.MetricsSnapshot snapshot3 = performanceMetrics.getSnapshot();
        assertEquals(1, snapshot3.activeCalculations());
    }
    
    @Test
    void testAverageCalculations() {
        // Record multiple batches with different processing times
        performanceMetrics.recordBatchStart("batch-1");
        try { Thread.sleep(50); } catch (InterruptedException e) { }
        performanceMetrics.recordBatchSuccess("batch-1", 100);
        
        performanceMetrics.recordBatchStart("batch-2");
        try { Thread.sleep(150); } catch (InterruptedException e) { }
        performanceMetrics.recordBatchSuccess("batch-2", 200);
        
        // Verify averages
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        assertEquals(2, snapshot.totalBatchesProcessed());
        assertEquals(300, snapshot.totalExposuresProcessed());
        assertEquals(150.0, snapshot.averageExposuresPerBatch());
        assertTrue(snapshot.averageProcessingTimeMillis() >= 100);
    }
    
    @Test
    void testCleanupOldBatchTimes() {
        // Record many batches
        for (int i = 0; i < 100; i++) {
            performanceMetrics.recordBatchStart("batch-" + i);
            performanceMetrics.recordBatchSuccess("batch-" + i, 10);
        }
        
        // Cleanup keeping only 50
        performanceMetrics.cleanupOldBatchTimes(50);
        
        // Verify metrics still work
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        assertEquals(100, snapshot.totalBatchesProcessed());
    }
    
    @Test
    void testThroughputCalculation() {
        // Record some batches
        performanceMetrics.recordBatchStart("batch-1");
        
        // Add a small delay to ensure measurable time passes
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        performanceMetrics.recordBatchSuccess("batch-1", 100);
        
        // Verify throughput is calculated (should be non-negative)
        PerformanceMetrics.MetricsSnapshot snapshot = performanceMetrics.getSnapshot();
        assertTrue(snapshot.throughputPerHour() >= 0, "Throughput should be non-negative");
    }
}
