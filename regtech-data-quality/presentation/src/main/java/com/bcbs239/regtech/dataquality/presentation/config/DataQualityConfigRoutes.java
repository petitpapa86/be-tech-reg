package com.bcbs239.regtech.dataquality.presentation.config;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.dataquality.presentation.common.Tags;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.*;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router configuration for data quality configuration endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 * 
 * <p>This follows the functional endpoints pattern used throughout the RegTech platform:
 * <ul>
 *   <li>Routes defined separately from controller logic</li>
 *   <li>Permission-based authorization via RouterAttributes</li>
 *   <li>OpenAPI tags for documentation</li>
 * </ul>
 */
@Component
public class DataQualityConfigRoutes {
    
    private final DataQualityConfigController controller;
    
    public DataQualityConfigRoutes(DataQualityConfigController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the data quality configuration endpoints with proper authentication and authorization.
     * 
     * <p>Endpoints:
     * <ul>
     *   <li>GET /api/v1/data-quality/config - Get current configuration</li>
     *   <li>PUT /api/v1/data-quality/config - Update configuration</li>
     *   <li>POST /api/v1/data-quality/config/reset - Reset to defaults</li>
     * </ul>
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            // Get current configuration
            route(GET("/api/v1/data-quality/config"), controller::getConfiguration),
            new String[]{"data-quality:config:view"},
            new String[]{Tags.DATA_QUALITY, Tags.CONFIGURATION},
            "Get current data quality configuration including thresholds, validation rules, and error handling policies"
        ).and(RouterAttributes.withAttributes(
            // Update configuration
            route(PUT("/api/v1/data-quality/config"), 
                    controller::updateConfiguration),
            new String[]{"data-quality:config:update"},
            new String[]{Tags.DATA_QUALITY, Tags.CONFIGURATION},
            "Update data quality configuration with new thresholds and validation rules"
        )).and(RouterAttributes.withAttributes(
            // Reset to defaults
            route(POST("/api/v1/data-quality/config/reset"), controller::resetToDefault),
            new String[]{"data-quality:config:reset"},
            new String[]{Tags.DATA_QUALITY, Tags.CONFIGURATION},
            "Reset data quality configuration to system defaults"
        ));
    }
}
