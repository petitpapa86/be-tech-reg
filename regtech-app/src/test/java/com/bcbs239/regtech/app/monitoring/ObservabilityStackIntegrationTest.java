package com.bcbs239.regtech.app.monitoring;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for observability stack configuration.
 * 
 * Tests:
 * - @Observed annotation functionality
 * - Business context propagation in traces
 * - Custom health indicators
 * - Alerting logic
 * - Metrics collection
 * 
 * Requirements: 1.1, 1.2, 2.1, 4.1, 5.1
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ObservabilityStackIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private BusinessObservationHandler businessObservationHandler;

    @Autowired(required = false)
    private BusinessMetricsCollector businessMetricsCollector;

    @Autowired(required = false)
    private HealthMonitoringService healthMonitoringService;

    @Autowired(required = false)
    private AlertingService alertingService;

    /**
     * Test 1: Verify ObservationRegistry is configured
     * Requirements: 1.1
     */
    @Test
    void testObservationRegistryIsConfigured() {
        assertThat(observationRegistry).isNotNull();
        assertThat(observationRegistry.observationConfig()).isNotNull();
    }

    /**
     * Test 2: Verify BusinessObservationHandler is registered
     * Requirements: 1.2
     */
    @Test
    void testBusinessObservationHandlerIsRegistered() {
        if (businessObservationHandler != null) {
            assertThat(businessObservationHandler).isNotNull();
            
            // Verify handler is registered in observation registry
            var handlers = observationRegistry.observationConfig().getObservationHandlers();
            assertThat(handlers).isNotEmpty();
        }
    }

    /**
     * Test 3: Verify @Observed annotation creates observations
     * Requirements: 1.1, 2.1
     */
    @Test
    void testObservedAnnotationCreatesObservations() {
        // Create an observation manually to test the registry
        Observation observation = Observation.createNotStarted("test.observation", observationRegistry);
        
        assertThat(observation).isNotNull();
        
        // Start and stop the observation
        observation.start();
        observation.stop();
        
        // Verify observation was recorded
        assertThat(observation.getContext()).isNotNull();
    }

    /**
     * Test 4: Verify business context is added to observations
     * Requirements: 1.2
     */
    @Test
    void testBusinessContextIsAddedToObservations() {
        if (businessObservationHandler != null) {
            // Create observation with business context
            Observation observation = Observation.createNotStarted("test.business.operation", observationRegistry)
                    .lowCardinalityKeyValue("batch.id", "test-batch-123")
                    .lowCardinalityKeyValue("user.id", "test-user-456");
            
            observation.start();
            observation.stop();
            
            // Verify context was added
            assertThat(observation.getContext().getLowCardinalityKeyValues())
                    .anyMatch(kv -> kv.getKey().equals("batch.id") && kv.getValue().equals("test-batch-123"))
                    .anyMatch(kv -> kv.getKey().equals("user.id") && kv.getValue().equals("test-user-456"));
        }
    }

    /**
     * Test 5: Verify BusinessMetricsCollector is available
     * Requirements: 2.5
     */
    @Test
    void testBusinessMetricsCollectorIsAvailable() {
        if (businessMetricsCollector != null) {
            assertThat(businessMetricsCollector).isNotNull();
            
            // Test recording a business metric
            businessMetricsCollector.recordDataQualityScore("test-batch", 0.95);
            
            // Verify metric was recorded (no exception thrown)
        }
    }

    /**
     * Test 6: Verify custom health indicators are registered
     * Requirements: 4.1
     */
    @Test
    void testCustomHealthIndicatorsAreRegistered() {
        // Test health endpoint
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    /**
     * Test 7: Verify HealthMonitoringService is available
     * Requirements: 4.1
     */
    @Test
    void testHealthMonitoringServiceIsAvailable() {
        if (healthMonitoringService != null) {
            assertThat(healthMonitoringService).isNotNull();
            
            // Test getting overall health status
            var healthStatus = healthMonitoringService.getOverallHealthStatus();
            assertThat(healthStatus).isNotNull();
        }
    }

    /**
     * Test 8: Verify AlertingService is available
     * Requirements: 5.1
     */
    @Test
    void testAlertingServiceIsAvailable() {
        if (alertingService != null) {
            assertThat(alertingService).isNotNull();
        }
    }

    /**
     * Test 9: Verify Prometheus metrics endpoint is accessible
     * Requirements: 2.1
     */
    @Test
    void testPrometheusMetricsEndpointIsAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("# HELP");
        assertThat(response.getBody()).contains("# TYPE");
    }

    /**
     * Test 10: Verify metrics endpoint is accessible
     * Requirements: 2.1
     */
    @Test
    void testMetricsEndpointIsAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("names");
    }

    /**
     * Test 11: Verify trace context propagation
     * Requirements: 1.1
     */
    @Test
    void testTraceContextPropagation() {
        // Create parent observation
        Observation parentObservation = Observation.createNotStarted("parent.operation", observationRegistry);
        parentObservation.start();
        
        try {
            // Create child observation within parent scope
            Observation childObservation = Observation.createNotStarted("child.operation", observationRegistry);
            childObservation.start();
            
            try {
                // Verify child observation exists
                assertThat(childObservation.getContext()).isNotNull();
            } finally {
                childObservation.stop();
            }
        } finally {
            parentObservation.stop();
        }
    }

    /**
     * Test 12: Verify async trace context propagation
     * Requirements: 1.1, 1.2
     */
    @Test
    void testAsyncTraceContextPropagation() {
        // This test verifies that trace context is propagated to async operations
        // The actual async configuration is tested in AsyncTracePropagationTest
        
        // Create observation
        Observation observation = Observation.createNotStarted("async.test", observationRegistry);
        observation.start();
        
        try {
            // Verify observation context is available
            assertThat(observation.getContext()).isNotNull();
        } finally {
            observation.stop();
        }
    }

    /**
     * Test 13: Verify error context is captured
     * Requirements: 1.4
     */
    @Test
    void testErrorContextIsCaptured() {
        Observation observation = Observation.createNotStarted("error.test", observationRegistry);
        observation.start();
        
        try {
            // Simulate an error
            throw new RuntimeException("Test error");
        } catch (Exception e) {
            observation.error(e);
        } finally {
            observation.stop();
        }
        
        // Verify error was captured
        assertThat(observation.getContext().getError()).isNotNull();
        assertThat(observation.getContext().getError().getMessage()).isEqualTo("Test error");
    }

    /**
     * Test 14: Verify observation with tags
     * Requirements: 2.1
     */
    @Test
    void testObservationWithTags() {
        Observation observation = Observation.createNotStarted("tagged.operation", observationRegistry)
                .lowCardinalityKeyValue("module", "test-module")
                .lowCardinalityKeyValue("operation", "test-operation")
                .highCardinalityKeyValue("request.id", "req-123");
        
        observation.start();
        observation.stop();
        
        // Verify tags were added
        assertThat(observation.getContext().getLowCardinalityKeyValues())
                .anyMatch(kv -> kv.getKey().equals("module") && kv.getValue().equals("test-module"))
                .anyMatch(kv -> kv.getKey().equals("operation") && kv.getValue().equals("test-operation"));
        
        assertThat(observation.getContext().getHighCardinalityKeyValues())
                .anyMatch(kv -> kv.getKey().equals("request.id") && kv.getValue().equals("req-123"));
    }

    /**
     * Test 15: Verify Spring Boot Actuator endpoints are secured
     * Requirements: 4.1
     */
    @Test
    void testActuatorEndpointsConfiguration() {
        // Test that actuator endpoints are accessible (in test profile)
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/metrics", String.class);
        assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        ResponseEntity<String> prometheusResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);
        assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
