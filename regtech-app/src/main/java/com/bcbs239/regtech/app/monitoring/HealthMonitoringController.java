package com.bcbs239.regtech.app.monitoring;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for health monitoring endpoints.
 * Provides access to aggregated health status and individual component health.
 * 
 * Requirements: 4.1, 4.5
 * - Expose aggregated health status
 * - Provide component-level health information
 * - Support cache management operations
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthMonitoringController {
    
    private final HealthMonitoringService healthMonitoringService;
    
    public HealthMonitoringController(HealthMonitoringService healthMonitoringService) {
        this.healthMonitoringService = healthMonitoringService;
    }
    
    /**
     * Gets the aggregated health status of all components.
     * 
     * @return Aggregated health status including overall status and component details
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        HealthMonitoringService.AggregatedHealthStatus status = healthMonitoringService.getAggregatedHealth();
        
        if (status.isOverallHealthy()) {
            return ResponseEntity.ok(status.toMap());
        } else {
            return ResponseEntity.status(503).body(status.toMap()); // Service Unavailable
        }
    }
    
    /**
     * Gets detailed health information including cache statistics.
     * 
     * @return Detailed health information with cache statistics
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        HealthMonitoringService.AggregatedHealthStatus status = healthMonitoringService.getAggregatedHealth();
        Map<String, Object> response = status.toMap();
        
        // Add cache statistics
        response.put("cacheStatistics", healthMonitoringService.getCacheStatistics());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Clears the health status cache to force fresh health checks.
     * 
     * @return Success message
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        healthMonitoringService.clearCache();
        return ResponseEntity.ok(Map.of(
            "message", "Health status cache cleared successfully",
            "timestamp", java.time.Instant.now().toString()
        ));
    }
    
    /**
     * Gets cache statistics.
     * 
     * @return Cache statistics including size, TTL, and cached components
     */
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        return ResponseEntity.ok(healthMonitoringService.getCacheStatistics());
    }
}