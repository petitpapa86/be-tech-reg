package com.bcbs239.regtech.modules.dataquality.presentation.routing;

import com.bcbs239.regtech.core.web.RouterAttributes;
import com.bcbs239.regtech.modules.dataquality.presentation.constants.Tags;
import com.bcbs239.regtech.modules.dataquality.presentation.controllers.QualityReportController;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.RequestPredicates.GET;

/**
 * Router configuration for quality report endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 */
@Component
public class QualityReportRoutes {
    
    private final QualityReportController controller;
    
    public QualityReportRoutes(QualityReportController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the quality report endpoints with proper authentication and authorization.
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/reports/{batchId}"), controller::getQualityReport),
            new String[]{"data-quality:reports:view"},
            new String[]{Tags.DATA_QUALITY, Tags.REPORTS},
            "Get quality report for a specific batch"
        ).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/data-quality/trends"), controller::getQualityTrends),
            new String[]{"data-quality:trends:view"},
            new String[]{Tags.DATA_QUALITY, Tags.TRENDS},
            "Get quality trends analysis for a bank over time"
        ));
    }
}