package com.bcbs239.regtech.reportgeneration.presentation.health;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.reportgeneration.presentation.common.Tags;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router configuration for report generation health and monitoring endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 * 
 * Requirements: 24.3, 24.4
 */
@Component
public class ReportGenerationHealthRoutes {
    
    private final ReportGenerationHealthController controller;
    
    public ReportGenerationHealthRoutes(ReportGenerationHealthController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the health and monitoring endpoints.
     * No authentication required for health checks to allow monitoring systems access.
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/report-generation/health"), controller::getModuleHealth),
            null, // No permissions required for health checks
            new String[]{Tags.REPORT_GENERATION, Tags.HEALTH},
            "Get comprehensive health status of the report generation module"
        ).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/report-generation/health/database"), controller::getDatabaseHealth),
            null,
            new String[]{Tags.REPORT_GENERATION, Tags.HEALTH},
            "Get database connectivity status"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/report-generation/health/s3"), controller::getS3Health),
            null,
            new String[]{Tags.REPORT_GENERATION, Tags.HEALTH},
            "Get S3 storage service availability status"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/report-generation/health/event-tracker"), controller::getEventTrackerHealth),
            null,
            new String[]{Tags.REPORT_GENERATION, Tags.HEALTH},
            "Get event tracker state status"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/report-generation/health/async-executor"), controller::getAsyncExecutorHealth),
            null,
            new String[]{Tags.REPORT_GENERATION, Tags.HEALTH},
            "Get async executor queue status"
        ));
    }
}
