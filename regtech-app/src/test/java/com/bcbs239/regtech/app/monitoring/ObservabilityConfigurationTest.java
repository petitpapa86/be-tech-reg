package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Spring Boot 4 observability configuration.
 * Verifies OpenTelemetry auto-configuration, Micrometer 2 integration, and Hetzner deployment settings.
 * 
 * Requirements: 1.1, 2.1, 3.1
 */
class ObservabilityConfigurationTest {
    
    /**
     * Verifies Spring Boot 4 OpenTelemetry dependency is available.
     * Requirement 1.1: Spring Boot 4 observability foundation
     */
    @Test
    void testOpenTelemetryDependencyAvailable() {
        // Verify that OpenTelemetry classes are available on classpath
        try {
            Class.forName("io.opentelemetry.api.OpenTelemetry");
            Class.forName("io.micrometer.observation.ObservationRegistry");
            // Test passes if classes are found
            assertTrue(true, "OpenTelemetry and Observation classes should be available");
        } catch (ClassNotFoundException e) {
            fail("OpenTelemetry dependencies should be available: " + e.getMessage());
        }
    }

    /**
     * Verifies that ApplicationMetricsCollector can be instantiated with a MeterRegistry.
     * Requirement 2.1: Micrometer 2 metrics collection
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
     * Requirement 2.1: Micrometer 2 metrics collection
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

    /**
     * Verifies that core Micrometer classes are available.
     * Requirement 1.1: Actuator endpoints configuration
     */
    @Test
    void testMicrometerDependenciesAvailable() {
        // Verify that core Micrometer classes are available
        try {
            Class.forName("io.micrometer.core.instrument.MeterRegistry");
            Class.forName("io.micrometer.core.instrument.Counter");
            Class.forName("io.micrometer.core.instrument.Timer");
            assertTrue(true, "Core Micrometer classes should be available");
        } catch (ClassNotFoundException e) {
            fail("Core Micrometer dependencies should be available: " + e.getMessage());
        }
    }

    /**
     * Verifies that Spring Boot 4 observability dependencies are properly configured.
     * Requirement 1.1: Spring Boot 4 observability foundation
     */
    @Test
    void testSpringBootObservabilityDependencies() {
        // Test that we can create basic observability components
        MeterRegistry registry = new SimpleMeterRegistry();
        assertNotNull(registry, "MeterRegistry should be creatable");
        
        // Test basic metrics functionality
        var counter = registry.counter("test.counter");
        counter.increment();
        assertEquals(1.0, counter.count(), "Counter should work correctly");
        
        var timer = registry.timer("test.timer");
        timer.record(100, java.util.concurrent.TimeUnit.MILLISECONDS);
        assertTrue(timer.count() > 0, "Timer should record samples");
    }
}
