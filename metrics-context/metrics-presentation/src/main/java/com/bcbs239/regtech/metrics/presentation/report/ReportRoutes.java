package com.bcbs239.regtech.metrics.presentation.report;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Component
public class ReportRoutes {

    private final ReportController reportController;

    public ReportRoutes(ReportController reportController) {
        this.reportController = reportController;
    }

    public RouterFunction<ServerResponse> reportRouter() {
        return reportController.mapEndpoint();
    }
}