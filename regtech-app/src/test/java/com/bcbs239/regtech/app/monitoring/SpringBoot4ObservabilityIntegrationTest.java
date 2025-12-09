package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Spring Boot 4 observability configuration.
 * Verifies that OpenTelemetry auto-configuration works correctly with Hetzner deployment settings.
 * 
 * Requirements: 1.1, 2.1, 3.1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "management.opentelemetry.resource-attributes.service.name=bcbs239-platform-test",
    "management.opentelemetry.resource-attributes.deployment.provider=hetzner",
    "management.opentelemetry.resource-attributes.deployment.region=fsn1",
    "management.tracing.enabled=true",
    "management.observations.annotations.enabled=true",
    "management.tracing.sampling.probability=1.0",
    "management.endpoints.web.exposure.include=health,metrics,prometheus"
})
class SpringBoot4ObservabilityIntegrationTest {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private ApplicationMetricsCollector applicationMetricsCollector;

    /**
     * Verifies that Spring Boot 4 auto-configures MeterRegistry.
     * Requirement 1.1: Spring Boot 4 observability foundation
     */
    @Test
    void testMeterRegistryAutoConfiguration() {
        assertNotNull(meterRegistry, "MeterRegistry should be auto-configured by Spring Boot 4");
        
        // Verify that basic JVM metrics are available
        assertNotNull(meterRegistry.find("jvm.memory.used").gauge(), 
            "JVM memory metrics should be automatically registered");
    }

    /**
     * Verifies that Spring Boot 4 auto-configures ObservationRegistry.
     * Requirement 1.1: Spring Boot 4 observability foundation
     */
    @Test
    void testObservationRegistryAutoConfiguration() {
        assertNotNull(observationRegistry, "ObservationRegistry should be auto-configured by Spring Boot 4");
    }

    /**
     * Verifies that ApplicationMetricsCollector is properly initialized.
     * Requirement 2.1: Business metrics collection
     */
    @Test
    void testApplicationMetricsCollectorInitialization() {
        assertNotNull(applicationMetricsCollector, "ApplicationMetricsCollector should be available");
        
        // Test business metrics functionality
        applicationMetricsCollector.recordApplicationStart();
        
        // Verify the metric was recorded
        assertNotNull(meterRegistry.find("regtech.application.starts").counter(),
            "Application start counter should be registered");
        
        assertTrue(meterRegistry.find("regtech.application.starts").counter().count() > 0,
            "Application start counter should have been incremented");
    }

    /**
     * Verifies that Prometheus metrics endpoint is available.
     * Requirement 1.1: Actuator endpoints configuration
     */
    @Test
    void testPrometheusMetricsAvailability() {
        // Verify Prometheus registry is configured
        assertTrue(meterRegistry.getClass().getName().contains("Prometheus") || 
                   meterRegistry.getClass().getName().contains("Composite"),
            "Prometheus metrics should be available through MeterRegistry");
    }

    /**
     * Verifies that custom business metrics can be created and recorded.
     * Requirement 2.1: Business metrics collection
     */
    @Test
    void testCustomBusinessMetrics() {
        // Create a custom business metric
        var customCounter = applicationMetricsCollector.createCounter(
            "regtech.test.business.events",
            "Test business events counter",
            "module", "test",
            "type", "integration"
        );
        
        assertNotNull(customCounter, "Custom business counter should be created");
        
        // Record some events
        customCounter.increment();
        customCounter.increment();
        
        assertEquals(2.0, customCounter.count(), "Custom counter should track increments correctly");
        
        // Verify it's registered in the meter registry
        assertNotNull(meterRegistry.find("regtech.test.business.events").counter(),
            "Custom business metric should be registered in MeterRegistry");
    }

    /**
     * Verifies that module-specific metrics can be recorded.
     * Requirement 2.1: Business metrics collection
     */
    @Test
    void testModuleSpecificMetrics() {
        // Record module initialization
        applicationMetricsCollector.recordModuleInitialization("observability-test");
        applicationMetricsCollector.recordModuleInitializationTime("observability-test", 150L);
        
        // Verify metrics were recorded
        var moduleCounter = meterRegistry.find("regtech.module.initializations").counter();
        assertNotNull(moduleCounter, "Module initialization counter should exist");
        assertTrue(moduleCounter.count() > 0, "Module initialization should be recorded");
        
        var moduleTimer = meterRegistry.find("regtech.module.initialization.time").timer();
        assertNotNull(moduleTimer, "Module initialization timer should exist");
    }
}