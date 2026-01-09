package com.bcbs239.regtech.riskcalculation.presentation.monitoring;

import com.bcbs239.regtech.riskcalculation.presentation.common.IEndpoint;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Health and monitoring controller for the risk calculation module.
 * Provides system health checks for database, storage, and metrics.
 * 
 * This controller orchestrates health checks by delegating to:
 * - RiskCalculationHealthChecker for component health checks
 * - RiskCalculationMetricsCollector for metrics collection
 * 
 * Requirements: 7.1, 7.2
 */
@Component
public class RiskCalculationHealthController implements IEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskCalculationHealthController.class);
    
    private final RiskCalculationHealthChecker healthChecker;
    private final RiskCalculationMetricsCollector metricsCollector;
    
    public RiskCalculationHealthController(
        RiskCalculationHealthChecker healthChecker,
        RiskCalculationMetricsCollector metricsCollector
    ) {
        this.healthChecker = healthChecker;
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * Maps the health and monitoring endpoints.
     * Note: This method is implemented for the IEndpoint interface but routing is 
     * handled by RiskCalculationHealthRoutes to avoid circular dependencies.
     */
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        // This is handled by RiskCalculationHealthRoutes to avoid circular dependency
        throw new UnsupportedOperationException(
            "Endpoint mapping is handled by RiskCalculationHealthRoutes component"
        );
    }
    
    /**
     * Get comprehensive health status of the risk calculation module.
     * Endpoint: GET /api/v1/risk-calculation/health
     */
    @Observed(name = "risk-calculation.api.health.module", contextualName = "get-module-health")
    public ServerResponse getModuleHealth(ServerRequest request) {
        logger.debug("Processing module health check request");
        
        var healthResult = healthChecker.checkModuleHealth();
        return ServerResponse.ok().body(healthResult);
    }
    
    /**
     * Get database connectivity status.
     * Endpoint: GET /api/v1/risk-calculation/health/database
     */
    @Observed(name = "risk-calculation.api.health.database", contextualName = "get-database-health")
    public ServerResponse getDatabaseHealth(ServerRequest request) {
        logger.debug("Processing database health check request");
        
        var result = healthChecker.checkDatabaseHealth();
        return ServerResponse.ok().body(result);
    }
    
    /**
     * Get file storage service availability status.
     * Endpoint: GET /api/v1/risk-calculation/health/storage
     */
    @Observed(name = "risk-calculation.api.health.storage", contextualName = "get-storage-health")
    public ServerResponse getStorageHealth(ServerRequest request) {
        logger.debug("Processing storage health check request");
        
        var result = healthChecker.checkStorageHealth();
        return ServerResponse.ok().body(result);
    }
    
    /**
     * Get risk calculation module metrics.
     * Endpoint: GET /api/v1/risk-calculation/metrics
     */
    @Observed(name = "risk-calculation.api.metrics", contextualName = "get-metrics")
    public ServerResponse getMetrics(ServerRequest request) {
        logger.debug("Processing metrics request");
        
        var metrics = metricsCollector.collectMetrics();
        return ServerResponse.ok().body(metrics);
    }
}