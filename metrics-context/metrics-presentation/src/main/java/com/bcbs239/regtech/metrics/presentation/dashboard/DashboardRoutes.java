package com.bcbs239.regtech.metrics.presentation.dashboard;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class DashboardRoutes {

    private final DashboardController dashboardController;

    public DashboardRoutes(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @Bean
    public RouterFunction<ServerResponse> dashboardRouter() {
        return dashboardController.mapEndpoint();
    }
}
