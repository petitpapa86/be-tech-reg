package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for observability configuration.
 * Verifies Micrometer 2 and OpenTelemetry integration.
 * 
 * Requirements: 10.1, 10.2, 10.3
 */
class ObservabilityConfigurationTest {
    
    /**
     * Verifies that ApplicationMetricsCollector can be instantiated with a MeterRegistry.
     * Requirement 10.1: Micrometer 2 metrics collection
     */
    @Test
    void testMetricsCollectorInstantiation() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationMetricsCollector metricsCollector = new ApplicationMetricsCollector(meterRegistry);
        
        assertNotNull(metricsCollector, "ApplicationMetricsCollector should be instantiated");
        
        // Initialize metrics
        metricsCollector.initializeMetrics();
    }
    
    /**
     * Verifies that custom metrics can be created.
     * Requirement 10.1: Micrometer 2 metrics collection
     */
    @Test
    void testCustomMetricsCreation() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationMetricsCollector metricsCollector = new ApplicationMetricsCollector(meterRegistry);
        metricsCollector.initializeMetrics();
        
        // Create a custom counter
        var counter = metricsCollector.createCounter(
            "test.counter",
            "Test counter",
            "type", "test"
        );
        
        assertNotNull(counter, "Custom counter should be created");
        
        // Increment and verify
        counter.increment();
        assertEquals(1.0, counter.count(), "Counter should be incremented");
    }
    
    /**
     * Verifies that custom timers can be created.
     * Requirement 10.1: Micrometer 2 metrics collection
     */
    @Test
    void testCustomTimerCreation() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationMetricsCollector metricsCollector = new ApplicationMetricsCollector(meterRegistry);
        metricsCollector.initializeMetrics();
        
        // Create a custom timer
        var timer = metricsCollector.createTimer(
            "test.timer",
            "Test timer",
            "type", "test"
        );
        
        assertNotNull(timer, "Custom timer should be created");
        
        // Record a sample
        timer.record(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        assertTrue(timer.count() > 0, "Timer should have recorded samples");
    }
    
    /**
     * Verifies that application startup metrics can be recorded.
     * Requirement 10.1: Micrometer 2 metrics collection
     */
    @Test
    void testApplicationStartupMetrics() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationMetricsCollector metricsCollector = new ApplicationMetricsCollector(meterRegistry);
        metricsCollector.initializeMetrics();
        
        // Record application start
        metricsCollector.recordApplicationStart();
        
        // Record startup time
        metricsCollector.recordApplicationStartupTime(1000L);
        
        // Verify metrics were recorded
        assertNotNull(meterRegistry.find("regtech.application.starts").counter());
        assertNotNull(meterRegistry.find("regtech.application.startup.time").timer());
    }
    
    /**
     * Verifies that module initialization metrics can be recorded.
     * Requirement 10.1: Micrometer 2 metrics collection
     */
    @Test
    void testModuleInitializationMetrics() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ApplicationMetricsCollector metricsCollector = new ApplicationMetricsCollector(meterRegistry);
        metricsCollector.initializeMetrics();
        
        // Record module initialization
        metricsCollector.recordModuleInitialization("test-module");
        metricsCollector.recordModuleInitializationTime("test-module", 500L);
        
        // Verify metrics were recorded
        assertNotNull(meterRegistry.find("regtech.module.initializations").counter());
        assertNotNull(meterRegistry.find("regtech.module.initialization.time").timer());
    }
}
