package com.bcbs239.regtech.reportgeneration.presentation.configuration;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.reportgeneration.presentation.common.Tags;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.*;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router configuration for report configuration endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 */
@Component
public class ReportConfigurationRoutes {
    
    private final ReportConfigurationController controller;
    
    public ReportConfigurationRoutes(ReportConfigurationController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the report configuration endpoints.
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/report-config"), controller::getReportingConfiguration),
            new String[]{"BCBS239_VIEW_REPORTS"},
            new String[]{Tags.REPORT_GENERATION, "Configuration"},
            "Get report generation configuration"
        ).and(RouterAttributes.withAttributes(
            route(PUT("/api/v1/report-config"), controller::updateReportingConfiguration),
            new String[]{"BCBS239_MANAGE_REPORTS"},
            new String[]{Tags.REPORT_GENERATION, "Configuration"},
            "Update report generation configuration"
        )).and(RouterAttributes.withAttributes(
            route(POST("/api/v1/reporting/reset"), controller::resetToDefault),
            new String[]{"BCBS239_MANAGE_REPORTS"},
            new String[]{Tags.REPORT_GENERATION, "Configuration"},
            "Reset report generation configuration to defaults"
        ));
    }
}
