package com.bcbs239.regtech.reportgeneration.presentation.health;

import com.bcbs239.regtech.reportgeneration.presentation.common.IEndpoint;
import com.bcbs239.regtech.reportgeneration.presentation.health.ReportGenerationHealthChecker.HealthCheckResult;
import com.bcbs239.regtech.reportgeneration.presentation.health.ReportGenerationHealthChecker.ModuleHealthResult;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Health and monitoring controller for the report generation module.
 * Provides system health checks for database, S3 storage, event tracker, and async executor.
 * 
 * This controller orchestrates health checks by delegating to:
 * - ReportGenerationHealthChecker for component health checks
 * - ReportGenerationHealthResponseHandler for response formatting
 * 
 * Requirements: 24.3, 24.4
 */
@Component
public class ReportGenerationHealthController implements IEndpoint {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportGenerationHealthController.class);
    
    private final ReportGenerationHealthChecker healthChecker;
    private final ReportGenerationHealthResponseHandler responseHandler;
    
    public ReportGenerationHealthController(
        ReportGenerationHealthChecker healthChecker,
        ReportGenerationHealthResponseHandler responseHandler
    ) {
        this.healthChecker = healthChecker;
        this.responseHandler = responseHandler;
    }
    
    /**
     * Maps the health and monitoring endpoints.
     * Note: This method is implemented for the IEndpoint interface but routing is 
     * handled by ReportGenerationHealthRoutes to avoid circular dependencies.
     */
    @Override
    public RouterFunction<ServerResponse> mapEndpoint() {
        // This is handled by ReportGenerationHealthRoutes to avoid circular dependency
        throw new UnsupportedOperationException(
            "Endpoint mapping is handled by ReportGenerationHealthRoutes component"
        );
    }
    
    /**
     * Get comprehensive health status of the report generation module.
     * Endpoint: GET /api/v1/report-generation/health
     */
    @Observed(name = "report-generation.api.health.module", contextualName = "get-module-health")
    public ServerResponse getModuleHealth(ServerRequest request) {
        logger.debug("Processing module health check request");
        
        ModuleHealthResult healthResult = healthChecker.checkModuleHealth();
        return responseHandler.handleModuleHealthResponse(healthResult);
    }
    
    /**
     * Get database connectivity status.
     * Endpoint: GET /api/v1/report-generation/health/database
     */
    @Observed(name = "report-generation.api.health.database", contextualName = "get-database-health")
    public ServerResponse getDatabaseHealth(ServerRequest request) {
        logger.debug("Processing database health check request");
        
        HealthCheckResult result = healthChecker.checkDatabaseHealth();
        return responseHandler.handleComponentHealthResponse("database", result);
    }
    
    /**
     * Get S3 storage service availability status.
     * Endpoint: GET /api/v1/report-generation/health/s3
     */
    @Observed(name = "report-generation.api.health.s3", contextualName = "get-s3-health")
    public ServerResponse getS3Health(ServerRequest request) {
        logger.debug("Processing S3 storage health check request");
        
        HealthCheckResult result = healthChecker.checkS3Accessibility();
        return responseHandler.handleComponentHealthResponse("s3-storage", result);
    }
    
    /**
     * Get event tracker state status.
     * Endpoint: GET /api/v1/report-generation/health/event-tracker
     */
    @Observed(name = "report-generation.api.health.event-tracker", contextualName = "get-event-tracker-health")
    public ServerResponse getEventTrackerHealth(ServerRequest request) {
        logger.debug("Processing event tracker health check request");
        
        HealthCheckResult result = healthChecker.checkEventTrackerState();
        return responseHandler.handleComponentHealthResponse("event-tracker", result);
    }
    
    /**
     * Get async executor queue status.
     * Endpoint: GET /api/v1/report-generation/health/async-executor
     */
    @Observed(name = "report-generation.api.health.async-executor", contextualName = "get-async-executor-health")
    public ServerResponse getAsyncExecutorHealth(ServerRequest request) {
        logger.debug("Processing async executor health check request");
        
        HealthCheckResult result = healthChecker.checkAsyncExecutorQueueSize();
        return responseHandler.handleComponentHealthResponse("async-executor", result);
    }
}
