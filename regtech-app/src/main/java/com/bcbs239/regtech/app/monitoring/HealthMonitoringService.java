package com.bcbs239.regtech.app.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Centralized health monitoring service for aggregating health status from all indicators.
 * Provides health check scheduling, status aggregation, and change detection.
 * 
 * Requirements: 4.1, 4.5
 * - Aggregate health status from all indicators
 * - Implement health check scheduling and caching
 * - Add health status change detection and logging
 */
@Service
public class HealthMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthMonitoringService.class);
    
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(30);
    
    private final List<HealthIndicator> healthIndicators;
    private final ExecutorService executorService;
    private final Map<String, CachedHealthStatus> healthStatusCache = new ConcurrentHashMap<>();
    private final Map<String, HealthStatus> previousHealthStatus = new ConcurrentHashMap<>();
    
    public HealthMonitoringService(List<HealthIndicator> healthIndicators) {
        this.healthIndicators = healthIndicators;
        this.executorService = Executors.newFixedThreadPool(5, r -> {
            Thread thread = new Thread(r, "health-monitor-");
            thread.setDaemon(true);
            return thread;
        });
        
        logger.info("Initialized HealthMonitoringService with {} health indicators", healthIndicators.size());
    }
    
    /**
     * Gets the aggregated health status of all components.
     */
    public AggregatedHealthStatus getAggregatedHealth() {
        Map<String, HealthStatus> componentHealth = new HashMap<>();
        boolean overallHealthy = true;
        int totalComponents = 0;
        int healthyComponents = 0;
        
        for (HealthIndicator indicator : healthIndicators) {
            String componentName = getComponentName(indicator);
            HealthStatus status = getComponentHealth(componentName, indicator);
            
            componentHealth.put(componentName, status);
            totalComponents++;
            
            if (status.isHealthy()) {
                healthyComponents++;
            } else {
                overallHealthy = false;
            }
        }
        
        return new AggregatedHealthStatus(
            overallHealthy,
            componentHealth,
            totalComponents,
            healthyComponents,
            Instant.now()
        );
    }
    
    /**
     * Gets the health status of a specific component with caching.
     */
    public HealthStatus getComponentHealth(String componentName, HealthIndicator indicator) {
        CachedHealthStatus cached = healthStatusCache.get(componentName);
        
        // Return cached result if still valid
        if (cached != null && cached.isValid()) {
            logger.debug("Returning cached health status for component: {}", componentName);
            return cached.status;
        }
        
        // Perform fresh health check
        HealthStatus status = performHealthCheck(componentName, indicator);
        
        // Cache the result
        healthStatusCache.put(componentName, new CachedHealthStatus(status));
        
        // Check for status changes and log them
        detectAndLogStatusChanges(componentName, status);
        
        return status;
    }
    
    /**
     * Scheduled health check that runs every 2 minutes.
     */
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void scheduledHealthCheck() {
        logger.debug("Starting scheduled health check for {} components", healthIndicators.size());
        
        List<CompletableFuture<Void>> futures = healthIndicators.stream()
            .map(indicator -> CompletableFuture.runAsync(() -> {
                String componentName = getComponentName(indicator);
                try {
                    getComponentHealth(componentName, indicator);
                } catch (Exception e) {
                    logger.error("Scheduled health check failed for component: {}", componentName, e);
                }
            }, executorService))
            .toList();
        
        // Wait for all health checks to complete (with timeout)
        CompletableFuture<Void> allChecks = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allChecks.get(HEALTH_CHECK_TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
            logger.debug("Completed scheduled health check for all components");
        } catch (Exception e) {
            logger.warn("Scheduled health check timed out or failed", e);
        }
    }
    
    /**
     * Performs the actual health check for a component.
     */
    private HealthStatus performHealthCheck(String componentName, HealthIndicator indicator) {
        Instant startTime = Instant.now();
        
        try {
            Health health = indicator.health();
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            return new HealthStatus(
                health.getStatus().getCode().equals("UP"),
                health.getStatus().getCode(),
                health.getDetails(),
                responseTime,
                startTime,
                null
            );
            
        } catch (Exception e) {
            Duration responseTime = Duration.between(startTime, Instant.now());
            logger.error("Health check failed for component: {}", componentName, e);
            
            return new HealthStatus(
                false,
                "DOWN",
                Map.of("error", e.getMessage(), "errorType", e.getClass().getSimpleName()),
                responseTime,
                startTime,
                e.getMessage()
            );
        }
    }
    
    /**
     * Detects health status changes and logs them.
     */
    private void detectAndLogStatusChanges(String componentName, HealthStatus currentStatus) {
        HealthStatus previousStatus = previousHealthStatus.get(componentName);
        
        if (previousStatus == null) {
            // First time checking this component
            logger.info("Initial health status for component '{}': {}", 
                componentName, currentStatus.statusCode);
        } else if (!previousStatus.statusCode.equals(currentStatus.statusCode)) {
            // Status changed
            logger.warn("Health status changed for component '{}': {} -> {} (response time: {}ms)", 
                componentName, 
                previousStatus.statusCode, 
                currentStatus.statusCode,
                currentStatus.responseTime.toMillis());
            
            // Log additional details for failures
            if (!currentStatus.isHealthy()) {
                logger.error("Component '{}' is now unhealthy. Details: {}", 
                    componentName, currentStatus.details);
            } else {
                logger.info("Component '{}' has recovered and is now healthy", componentName);
            }
        }
        
        // Update previous status
        previousHealthStatus.put(componentName, currentStatus);
    }
    
    /**
     * Gets a human-readable component name from the health indicator.
     */
    private String getComponentName(HealthIndicator indicator) {
        String className = indicator.getClass().getSimpleName();
        
        // Remove "HealthIndicator" suffix if present
        if (className.endsWith("HealthIndicator")) {
            className = className.substring(0, className.length() - "HealthIndicator".length());
        }
        
        // Convert CamelCase to kebab-case
        return className.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
    
    /**
     * Clears the health status cache (useful for testing or forced refresh).
     */
    public void clearCache() {
        healthStatusCache.clear();
        logger.info("Health status cache cleared");
    }
    
    /**
     * Gets cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", healthStatusCache.size());
        stats.put("cacheTtlSeconds", CACHE_TTL.getSeconds());
        stats.put("totalComponents", healthIndicators.size());
        stats.put("cachedComponents", healthStatusCache.keySet());
        return stats;
    }
    
    /**
     * Represents the health status of a single component.
     */
    public static class HealthStatus {
        private final boolean healthy;
        private final String statusCode;
        private final Map<String, Object> details;
        private final Duration responseTime;
        private final Instant timestamp;
        private final String errorMessage;
        
        public HealthStatus(boolean healthy, String statusCode, Map<String, Object> details,
                          Duration responseTime, Instant timestamp, String errorMessage) {
            this.healthy = healthy;
            this.statusCode = statusCode;
            this.details = details != null ? Map.copyOf(details) : Map.of();
            this.responseTime = responseTime;
            this.timestamp = timestamp;
            this.errorMessage = errorMessage;
        }
        
        public boolean isHealthy() { return healthy; }
        public String getStatusCode() { return statusCode; }
        public Map<String, Object> getDetails() { return details; }
        public Duration getResponseTime() { return responseTime; }
        public Instant getTimestamp() { return timestamp; }
        public String getErrorMessage() { return errorMessage; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("healthy", healthy);
            map.put("status", statusCode);
            map.put("responseTimeMs", responseTime.toMillis());
            map.put("timestamp", timestamp.toString());
            map.put("details", details);
            if (errorMessage != null) {
                map.put("error", errorMessage);
            }
            return map;
        }
    }
    
    /**
     * Represents the aggregated health status of all components.
     */
    public static class AggregatedHealthStatus {
        private final boolean overallHealthy;
        private final Map<String, HealthStatus> componentHealth;
        private final int totalComponents;
        private final int healthyComponents;
        private final Instant timestamp;
        
        public AggregatedHealthStatus(boolean overallHealthy, Map<String, HealthStatus> componentHealth,
                                    int totalComponents, int healthyComponents, Instant timestamp) {
            this.overallHealthy = overallHealthy;
            this.componentHealth = Map.copyOf(componentHealth);
            this.totalComponents = totalComponents;
            this.healthyComponents = healthyComponents;
            this.timestamp = timestamp;
        }
        
        public boolean isOverallHealthy() { return overallHealthy; }
        public Map<String, HealthStatus> getComponentHealth() { return componentHealth; }
        public int getTotalComponents() { return totalComponents; }
        public int getHealthyComponents() { return healthyComponents; }
        public int getUnhealthyComponents() { return totalComponents - healthyComponents; }
        public Instant getTimestamp() { return timestamp; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("overallHealthy", overallHealthy);
            map.put("totalComponents", totalComponents);
            map.put("healthyComponents", healthyComponents);
            map.put("unhealthyComponents", getUnhealthyComponents());
            map.put("timestamp", timestamp.toString());
            
            Map<String, Object> components = new HashMap<>();
            componentHealth.forEach((name, status) -> components.put(name, status.toMap()));
            map.put("components", components);
            
            return map;
        }
    }
    
    /**
     * Cached health status with TTL.
     */
    private static class CachedHealthStatus {
        private final HealthStatus status;
        private final Instant timestamp;
        
        public CachedHealthStatus(HealthStatus status) {
            this.status = status;
            this.timestamp = Instant.now();
        }
        
        public boolean isValid() {
            return Duration.between(timestamp, Instant.now()).compareTo(CACHE_TTL) < 0;
        }
    }
}