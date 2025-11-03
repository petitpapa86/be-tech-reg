package com.bcbs239.regtech.modules.dataquality.presentation.monitoring;

import com.bcbs239.regtech.modules.dataquality.presentation.monitoring.QualityHealthChecker.HealthCheckResult;
import com.bcbs239.regtech.modules.dataquality.presentation.monitoring.QualityHealthChecker.ModuleHealthResult;
import com.bcbs239.regtech.modules.dataquality.presentation.monitoring.QualityMetricsCollector.QualityMetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.util.Map;

/**
 * Handler for converting health check results and metrics to HTTP responses.
 * Provides consistent response formatting for health and monitoring endpoints.
 */
@Component
public class QualityHealthResponseHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityHealthResponseHandler.class);
    
    /**
     * Converts module health result to HTTP response.
     */
    public ServerResponse handleModuleHealthResponse(ModuleHealthResult healthResult) {
        try {
            int httpStatus = healthResult.isHealthy() ? 200 : 503;
            
            logger.debug("Module health check completed with status: {}", healthResult.overallStatus());
            
            return ServerResponse.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(healthResult.toResponseMap());
                
        } catch (Exception e) {
            logger.error("Error formatting module health response: {}", e.getMessage(), e);
            return handleHealthCheckError("module health check", e);
        }
    }
    
    /**
     * Converts individual component health result to HTTP response.
     */
    public ServerResponse handleComponentHealthResponse(String componentName, HealthCheckResult result) {
        try {
            Map<String, Object> response = Map.of(
                "component", componentName,
                "status", result.status(),
                "message", result.message(),
                "timestamp", Instant.now().toString(),
                "details", result.details()
            );
            
            int httpStatus = result.isHealthy() ? 200 : 503;
            
            return ServerResponse.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
                
        } catch (Exception e) {
            logger.error("Error formatting {} health response: {}", componentName, e.getMessage(), e);
            return handleHealthCheckError(componentName + " health check", e);
        }
    }
    
    /**
     * Converts metrics to HTTP response.
     */
    public ServerResponse handleMetricsResponse(QualityMetrics metrics) {
        try {
            return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(metrics.toResponseMap());
                
        } catch (Exception e) {
            logger.error("Error formatting metrics response: {}", e.getMessage(), e);
            return handleMetricsError(e);
        }
    }
    
    /**
     * Handles health check errors consistently.
     */
    private ServerResponse handleHealthCheckError(String operation, Exception e) {
        Map<String, Object> errorResponse = Map.of(
            "status", "DOWN",
            "error", operation + " failed: " + e.getMessage(),
            "timestamp", Instant.now().toString()
        );
        
        return ServerResponse.status(503)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorResponse);
    }
    
    /**
     * Handles metrics collection errors consistently.
     */
    private ServerResponse handleMetricsError(Exception e) {
        Map<String, Object> errorResponse = Map.of(
            "module", "data-quality",
            "error", "Failed to collect metrics: " + e.getMessage(),
            "timestamp", Instant.now().toString()
        );
        
        return ServerResponse.status(500)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorResponse);
    }
}