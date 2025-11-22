package com.bcbs239.regtech.reportgeneration.presentation.health;

import com.bcbs239.regtech.reportgeneration.presentation.health.ReportGenerationHealthChecker.HealthCheckResult;
import com.bcbs239.regtech.reportgeneration.presentation.health.ReportGenerationHealthChecker.ModuleHealthResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

/**
 * Handles formatting of health check responses.
 * Converts health check results into appropriate HTTP responses.
 */
@Component
public class ReportGenerationHealthResponseHandler {
    
    /**
     * Handles module-wide health check response.
     * Returns 200 for UP, 503 for DOWN, 200 for WARN (with warning indicator).
     */
    public ServerResponse handleModuleHealthResponse(ModuleHealthResult healthResult) {
        HttpStatus httpStatus = determineHttpStatus(healthResult.overallStatus());
        
        return ServerResponse.status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .body(healthResult.toResponseMap());
    }
    
    /**
     * Handles individual component health check response.
     */
    public ServerResponse handleComponentHealthResponse(String componentName, HealthCheckResult result) {
        HttpStatus httpStatus = determineHttpStatus(result.status());
        
        Map<String, Object> response = Map.of(
            "component", componentName,
            "status", result.status(),
            "message", result.message(),
            "details", result.details()
        );
        
        return ServerResponse.status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }
    
    /**
     * Determines HTTP status code based on health status.
     * UP and WARN return 200, DOWN returns 503.
     */
    private HttpStatus determineHttpStatus(String healthStatus) {
        return switch (healthStatus) {
            case "DOWN" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "WARN", "UP" -> HttpStatus.OK;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
