package com.bcbs239.regtech.metrics.presentation.dashboard;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Component
public class DashboardRoutes {

    private final DashboardController dashboardController;

    public DashboardRoutes(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public RouterFunction<ServerResponse> dashboardRouter() {
        return dashboardController.mapEndpoint();
    }
}
