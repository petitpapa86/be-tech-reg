package com.bcbs239.regtech.riskcalculation.presentation.monitoring;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.riskcalculation.presentation.common.Tags;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router configuration for risk calculation health and monitoring endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 */
@Component
public class RiskCalculationHealthRoutes {
    
    private final RiskCalculationHealthController controller;
    
    public RiskCalculationHealthRoutes(RiskCalculationHealthController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the health and monitoring endpoints.
     * No authentication required for health checks, but metrics require permissions.
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/health"), controller::getModuleHealth),
            null, // No permissions required for health checks
            new String[]{Tags.RISK_CALCULATION, Tags.HEALTH},
            "Get comprehensive health status of the risk calculation module"
        ).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/health/database"), controller::getDatabaseHealth),
            null,
            new String[]{Tags.RISK_CALCULATION, Tags.HEALTH},
            "Get database connectivity status"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/health/file-storage"), controller::getFileStorageHealth),
            null,
            new String[]{Tags.RISK_CALCULATION, Tags.HEALTH},
            "Get file storage service availability status"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/health/currency-conversion"), controller::getCurrencyConversionHealth),
            null,
            new String[]{Tags.RISK_CALCULATION, Tags.HEALTH},
            "Get currency conversion service status"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/metrics"), controller::getPerformanceMetrics),
            new String[]{"risk-calculation:metrics:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.METRICS},
            "Get performance metrics and statistics"
        ));
    }
}
