package com.bcbs239.regtech.riskcalculation.presentation.monitoring;

import com.bcbs239.regtech.riskcalculation.presentation.common.IEndpoint;
import com.bcbs239.regtech.riskcalculation.presentation.monitoring.RiskCalculationHealthChecker.HealthCheckResult;
import com.bcbs239.regtech.riskcalculation.presentation.monitoring.RiskCalculationHealthChecker.ModuleHealthResult;
import com.bcbs239.regtech.riskcalculation.presentation.monitoring.RiskCalculationMetricsCollector.RiskCalculationMetrics;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Health and monitoring controller for the risk calculation module.
 * Provides system health checks, database connectivity, file storage availability, and performance metrics.
 * 
 * This controller focuses on orchestrating health checks and metrics collection by delegating to:
 * - RiskCalculationHealthChecker for component health checks
 * - RiskCalculationMetricsCollector for performance metrics
 * - RiskCalculationHealthResponseHandler for response formatting
 * 
 * Requirements: 9.5
 */
@Component
public class RiskCalculationHealthController implements IEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskCalculationHealthController.class);
    
    private final RiskCalculationHealthChecker healthChecker;
    private final RiskCalculationMetricsCollector metricsCollector;
    private final RiskCalculationHealthResponseHandler responseHandler;
    
    public RiskCalculationHealthController(
        RiskCalculationHealthChecker healthChecker,
        RiskCalculationMetricsCollector metricsCollector,
        RiskCalculationHealthResponseHandler responseHandler
    ) {
        this.healthChecker = healthChecker;
        this.metricsCollector = metricsCollector;
        this.responseHandler = responseHandler;
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
        
        ModuleHealthResult healthResult = healthChecker.checkModuleHealth();
        return responseHandler.handleModuleHealthResponse(healthResult);
    }
    
    /**
     * Get database connectivity status.
     * Endpoint: GET /api/v1/risk-calculation/health/database
     */
    @Observed(name = "risk-calculation.api.health.database", contextualName = "get-database-health")
    public ServerResponse getDatabaseHealth(ServerRequest request) {
        logger.debug("Processing database health check request");
        
        HealthCheckResult result = healthChecker.checkDatabaseHealth();
        return responseHandler.handleComponentHealthResponse("database", result);
    }
    
    /**
     * Get file storage service availability status.
     * Endpoint: GET /api/v1/risk-calculation/health/file-storage
     */
    @Observed(name = "risk-calculation.api.health.file-storage", contextualName = "get-file-storage-health")
    public ServerResponse getFileStorageHealth(ServerRequest request) {
        logger.debug("Processing file storage health check request");
        
        HealthCheckResult result = healthChecker.checkFileStorageHealth();
        return responseHandler.handleComponentHealthResponse("file-storage", result);
    }
    
    /**
     * Get currency conversion service status.
     * Endpoint: GET /api/v1/risk-calculation/health/currency-conversion
     */
    @Observed(name = "risk-calculation.api.health.currency-conversion", contextualName = "get-currency-conversion-health")
    public ServerResponse getCurrencyConversionHealth(ServerRequest request) {
        logger.debug("Processing currency conversion health check request");
        
        HealthCheckResult result = healthChecker.checkCurrencyApiHealth();
        return responseHandler.handleComponentHealthResponse("currency-conversion", result);
    }
    
    /**
     * Get performance metrics and statistics.
     * Endpoint: GET /api/v1/risk-calculation/metrics
     */
    @Observed(name = "risk-calculation.api.metrics.performance", contextualName = "get-performance-metrics")
    public ServerResponse getPerformanceMetrics(ServerRequest request) {
        logger.debug("Processing performance metrics request");
        
        RiskCalculationMetrics metrics = metricsCollector.collectMetrics();
        return responseHandler.handleMetricsResponse(metrics);
    }
}
