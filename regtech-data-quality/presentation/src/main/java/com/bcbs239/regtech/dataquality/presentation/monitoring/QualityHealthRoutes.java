package com.bcbs239.regtech.dataquality.presentation.monitoring;

import com.bcbs239.regtech.core.web.RouterAttributes;
import com.bcbs239.regtech.dataquality.presentation.common.Tags;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.GET;

/**
 * Router configuration for quality health and monitoring endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 */
@Component
public class QualityHealthRoutes {
    
    private final QualityHealthController controller;
    
    public QualityHealthRoutes(QualityHealthController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the health and monitoring endpoints.
     * No authentication required for health checks, but metrics require permissions.
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/health"), controller::getModuleHealth),
            null, // No permissions required for health checks
            new String[]{Tags.DATA_QUALITY, Tags.HEALTH},
            "Get comprehensive health status of the data quality module"
        ).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/health/database"), controller::getDatabaseHealth),
            null,
            new String[]{Tags.DATA_QUALITY, Tags.HEALTH},
            "Get database connectivity status"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/health/s3"), controller::getS3Health),
            null,
            new String[]{Tags.DATA_QUALITY, Tags.HEALTH},
            "Get S3 storage service availability status"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/health/validation-engine"), controller::getValidationEngineHealth),
            null,
            new String[]{Tags.DATA_QUALITY, Tags.HEALTH},
            "Get validation engine status and performance metrics"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/metrics"), controller::getPerformanceMetrics),
            new String[]{"data-quality:metrics:view"},
            new String[]{Tags.DATA_QUALITY, Tags.METRICS},
            "Get performance metrics and statistics"
        ));
    }
}