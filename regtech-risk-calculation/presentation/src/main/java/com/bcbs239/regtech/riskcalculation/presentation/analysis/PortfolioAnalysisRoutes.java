package com.bcbs239.regtech.riskcalculation.presentation.analysis;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import com.bcbs239.regtech.riskcalculation.presentation.common.Tags;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router configuration for portfolio analysis endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 * 
 * Requirements: 6.1, 6.2, 6.3
 */
@Component
public class PortfolioAnalysisRoutes {
    
    private final PortfolioAnalysisController controller;
    
    public PortfolioAnalysisRoutes(PortfolioAnalysisController controller) {
        this.controller = controller;
    }
    
    /**
     * Maps the portfolio analysis endpoints.
     * Requires appropriate permissions for accessing portfolio analysis data.
     */
    public RouterFunction<ServerResponse> createRoutes() {
        return RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/portfolio-analysis/{batchId}"), controller::getPortfolioAnalysis),
            new String[]{"risk-calculation:portfolio:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.PORTFOLIO_ANALYSIS},
            "Get complete portfolio analysis for a batch"
        ).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/portfolio-analysis/{batchId}/concentrations"), controller::getConcentrationIndices),
            new String[]{"risk-calculation:portfolio:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.PORTFOLIO_ANALYSIS},
            "Get concentration indices for a batch"
        )).and(RouterAttributes.withAttributes(
            route(GET("/api/v1/risk-calculation/portfolio-analysis/{batchId}/breakdowns"), controller::getBreakdownsByType),
            new String[]{"risk-calculation:portfolio:view"},
            new String[]{Tags.RISK_CALCULATION, Tags.PORTFOLIO_ANALYSIS},
            "Get breakdowns for a batch with optional type filter"
        ));
    }
}
