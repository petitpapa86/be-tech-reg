package com.bcbs239.regtech.app.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for the health monitoring system components.
 * Verifies health indicator functionality and aggregation logic.
 */
class HealthMonitoringSystemTest {
    
    private HealthMonitoringService healthMonitoringService;
    
    @BeforeEach
    void setUp() {
        // Create mock health indicators
        List<HealthIndicator> indicators = List.of(
            new MockHealthIndicator("healthy", true),
            new MockHealthIndicator("unhealthy", false)
        );
        
        healthMonitoringService = new HealthMonitoringService(indicators);
    }
    
    @Test
    void testAggregatedHealthWithMixedComponents() {
        // When getting aggregated health
        HealthMonitoringService.AggregatedHealthStatus status = healthMonitoringService.getAggregatedHealth();
        
        // Then overall health should be false (due to unhealthy component)
        assertFalse(status.isOverallHealthy());
        assertEquals(2, status.getTotalComponents());
        assertEquals(1, status.getHealthyComponents());
        assertEquals(1, status.getUnhealthyComponents());
        
        // And component details should be available
        Map<String, HealthMonitoringService.HealthStatus> componentHealth = status.getComponentHealth();
        assertTrue(componentHealth.containsKey("mock-healthy"));
        assertTrue(componentHealth.containsKey("mock-unhealthy"));
        
        assertTrue(componentHealth.get("mock-healthy").isHealthy());
        assertFalse(componentHealth.get("mock-unhealthy").isHealthy());
    }
    
    @Test
    void testHealthStatusCaching() {
        // Given a component
        MockHealthIndicator indicator = new MockHealthIndicator("test", true);
        
        // When checking health multiple times quickly
        HealthMonitoringService.HealthStatus status1 = healthMonitoringService.getComponentHealth("test", indicator);
        HealthMonitoringService.HealthStatus status2 = healthMonitoringService.getComponentHealth("test", indicator);
        
        // Then both should be healthy
        assertTrue(status1.isHealthy());
        assertTrue(status2.isHealthy());
        
        // And response times should be reasonable
        assertTrue(status1.getResponseTime().compareTo(Duration.ofSeconds(1)) < 0);
        assertTrue(status2.getResponseTime().compareTo(Duration.ofSeconds(1)) < 0);
    }
    
    @Test
    void testCacheStatistics() {
        // When getting cache statistics
        Map<String, Object> stats = healthMonitoringService.getCacheStatistics();
        
        // Then statistics should be available
        assertNotNull(stats);
        assertTrue(stats.containsKey("cacheSize"));
        assertTrue(stats.containsKey("cacheTtlSeconds"));
        assertTrue(stats.containsKey("totalComponents"));
        
        assertEquals(2, stats.get("totalComponents"));
    }
    
    @Test
    void testCacheClear() {
        // Given some cached health status
        healthMonitoringService.getAggregatedHealth();
        
        // When clearing cache
        healthMonitoringService.clearCache();
        
        // Then cache should be empty
        Map<String, Object> stats = healthMonitoringService.getCacheStatistics();
        assertEquals(0, stats.get("cacheSize"));
    }
    
    /**
     * Mock health indicator for testing.
     */
    private static class MockHealthIndicator implements HealthIndicator {
        private final String name;
        private final boolean healthy;
        
        public MockHealthIndicator(String name, boolean healthy) {
            this.name = name;
            this.healthy = healthy;
        }
        
        @Override
        public Health health() {
            if (healthy) {
                return Health.up()
                    .withDetail("component", name)
                    .withDetail("status", "operational")
                    .build();
            } else {
                return Health.down()
                    .withDetail("component", name)
                    .withDetail("error", "simulated failure")
                    .build();
            }
        }
    }
}