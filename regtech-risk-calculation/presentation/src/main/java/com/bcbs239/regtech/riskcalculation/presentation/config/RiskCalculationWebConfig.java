package com.bcbs239.regtech.riskcalculation.presentation.config;

import com.bcbs239.regtech.riskcalculation.presentation.monitoring.RiskCalculationHealthRoutes;
import com.bcbs239.regtech.riskcalculation.presentation.status.BatchSummaryStatusRoutes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Web configuration for the risk calculation module.
 * Registers all functional endpoints and configures routing.
 */
@Configuration
public class RiskCalculationWebConfig {
    
    /**
     * Registers all risk calculation module endpoints.
     */
    @Bean
    public RouterFunction<ServerResponse> riskCalculationRoutes(
        RiskCalculationHealthRoutes healthRoutes,
        BatchSummaryStatusRoutes statusRoutes
    ) {
        return healthRoutes.createRoutes()
            .and(statusRoutes.createRoutes());
    }
}
