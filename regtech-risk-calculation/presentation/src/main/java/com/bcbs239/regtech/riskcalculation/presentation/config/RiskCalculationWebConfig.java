package com.bcbs239.regtech.riskcalculation.presentation.config;

import com.bcbs239.regtech.riskcalculation.presentation.monitoring.RiskCalculationHealthRoutes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Web configuration for the risk calculation module.
 * Registers functional endpoints for health monitoring.
 * 
 * Note: Most controllers now use @RestController with @RequestMapping annotations
 * for routing (PortfolioAnalysisController, ExposureResultsController, BatchStatusController).
 * Only health monitoring endpoints use functional routing.
 */
@Configuration
public class RiskCalculationWebConfig {
    
    /**
     * Registers health monitoring endpoints using functional routing.
     * Other endpoints are handled by @RestController annotations.
     */
    @Bean
    public RouterFunction<ServerResponse> riskCalculationRoutes(
        RiskCalculationHealthRoutes healthRoutes
    ) {
        return healthRoutes.createRoutes();
    }
}
