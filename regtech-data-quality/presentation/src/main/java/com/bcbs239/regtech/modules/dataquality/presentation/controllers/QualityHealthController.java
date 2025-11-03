package com.bcbs239.regtech.modules.dataquality.presentation.controllers;

import com.bcbs239.regtech.modules.dataquality.presentation.common.IEndpoint;
import com.bcbs239.regtech.modules.dataquality.presentation.handlers.QualityHealthResponseHandler;
import com.bcbs239.regtech.modules.dataquality.presentation.health.QualityHealthChecker;
import com.bcbs239.regtech.modules.dataquality.presentation.health.QualityHealthChecker.HealthCheckResult;
import com.bcbs239.regtech.modules.dataquality.presentation.health.QualityHealthChecker.ModuleHealthResult;
import com.bcbs239.regtech.modules.dataquality.presentation.metrics.QualityMetricsCollector;
import com.bcbs239.regtech.modules.dataquality.presentation.metrics.QualityMetricsCollector.QualityMetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Health and monitoring controller for the data quality module.
 * Provides system health checks, database connectivity, S3 availability, and performance metrics.
 * 
 * This controller focuses on orchestrating health checks and metrics collection by delegating to:
 * - QualityHealthChecker for component health checks
 * - QualityMetricsCollector for performance metrics
 * - QualityHealthResponseHandler for response formatting
 * 
 * Requirements: 9.5, 9.6
 */
@Component
public class QualityHealthController implements IEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityHealthController.class);
    
    private final QualityHealthChecker healthChecker;
    private final QualityMetricsCollector metricsCollector;
    private final QualityHealthResponseHandler responseHandler;
    
    public QualityHealthController(
        QualityHealthChecker healthChecker,
        QualityMetricsCollector metricsCollector,
        QualityHealthResponseHandler responseHandler
    ) {
        this.healthChecker = healthChecker;
        this.metricsCollector = metricsCollector;
        this.responseHandler = responseHandler;
    }
    
    /**
     * Maps the health and monitoring endpoints.
     * Note: This method is implemented for the IEndpoint interface but routing is 
     * handled by QualityHealthRoutes to avoid circular dependencies.
     */
    @Override
    public RouterFunction<ServerResponse> mapEndpoints() {
        // This is handled by QualityHealthRoutes to avoid circular dependency
        throw new UnsupportedOperationException(
            "Endpoint mapping is handled by QualityHealthRoutes component"
        );
    }
    
    /**
     * Get comprehensive health status of the data quality module.
     * Endpoint: GET /api/v1/data-quality/health
     */
    public ServerResponse getModuleHealth(ServerRequest request) {
        logger.debug("Processing module health check request");
        
        ModuleHealthResult healthResult = healthChecker.checkModuleHealth();
        return responseHandler.handleModuleHealthResponse(healthResult);
    }
    
    /**
     * Get database connectivity status.
     * Endpoint: GET /api/v1/data-quality/health/database
     */
    public ServerResponse getDatabaseHealth(ServerRequest request) {
        logger.debug("Processing database health check request");
        
        HealthCheckResult result = healthChecker.checkDatabaseHealth();
        return responseHandler.handleComponentHealthResponse("database", result);
    }
    
    /**
     * Get S3 storage service availability status.
     * Endpoint: GET /api/v1/data-quality/health/s3
     */
    public ServerResponse getS3Health(ServerRequest request) {
        logger.debug("Processing S3 health check request");
        
        HealthCheckResult result = healthChecker.checkS3Health();
        return responseHandler.handleComponentHealthResponse("s3", result);
    }
    
    /**
     * Get validation engine status and performance metrics.
     * Endpoint: GET /api/v1/data-quality/health/validation-engine
     */
    public ServerResponse getValidationEngineHealth(ServerRequest request) {
        logger.debug("Processing validation engine health check request");
        
        HealthCheckResult result = healthChecker.checkValidationEngineHealth();
        return responseHandler.handleComponentHealthResponse("validation-engine", result);
    }
    
    /**
     * Get performance metrics and statistics.
     * Endpoint: GET /api/v1/data-quality/metrics
     */
    public ServerResponse getPerformanceMetrics(ServerRequest request) {
        logger.debug("Processing performance metrics request");
        
        QualityMetrics metrics = metricsCollector.collectMetrics();
        return responseHandler.handleMetricsResponse(metrics);
    }

}