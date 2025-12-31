package com.bcbs239.regtech.metrics.presentation.dashboard;

import org.springframework.http.MediaType;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import com.bcbs239.regtech.metrics.presentation.dashboard.dto.DashboardResponse;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.FileItem;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.ComplianceState;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.ReportItem;

@Component
public class DashboardController {

    public RouterFunction<ServerResponse> mapEndpoint() {
        return RouterFunctions.route(
                RequestPredicates.GET("/api/v1/metrics/dashboard"),
                this::getDashboard
        );
    }

    public ServerResponse getDashboard(ServerRequest request) {
        DashboardResponse.Summary summary = new DashboardResponse.Summary(34, 92.0, 87, 156);

        List<FileItem> files = List.of(
                new FileItem("esposizioni_settembre.xlsx", "15 Set 2025", 87.2, "VIOLATIONS"),
                new FileItem("grandi_esposizioni_agosto.xlsx", "28 Ago 2025", 94.1, "COMPLIANT")
        );

        ComplianceState compliance = new ComplianceState(89.0, 94.0, 87.0, 96.0);

        List<ReportItem> reports = List.of(
                new ReportItem("BCBS_239_Report_Settembre.pdf", "COMPLETED", "Generato: 28 Ago 2025, 09:23 • 1,8 MB • 12 pagine"),
                new ReportItem("Report_Agosto.pdf", "GENERATING", "Generato: 02 Ago 2025, 14:02 • 0,9 MB • 8 pagine")
        );

        Integer lastBatchViolations = 12;

        DashboardResponse dashboard = new DashboardResponse(summary, files, compliance, reports, lastBatchViolations);

        Object api = ResponseUtils.success(dashboard);

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(api);
    }
}
