package com.bcbs239.regtech.metrics.presentation.config;

import com.bcbs239.regtech.metrics.presentation.dashboard.DashboardRoutes;
import com.bcbs239.regtech.metrics.presentation.report.ReportRoutes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Web configuration for the metrics module.
 * Registers all functional endpoints and configures routing.
 */
@Configuration
public class MetricsWebConfig {

    /**
     * Registers all metrics module endpoints.
     */
    @Bean
    public RouterFunction<ServerResponse> metricsRoutes(
        DashboardRoutes dashboardRoutes,
        ReportRoutes reportRoutes
    ) {
        return dashboardRoutes.dashboardRouter()
                .and(reportRoutes.reportRouter());
    }
}