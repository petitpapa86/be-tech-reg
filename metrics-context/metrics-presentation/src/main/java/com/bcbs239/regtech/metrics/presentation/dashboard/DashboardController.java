package com.bcbs239.regtech.metrics.presentation.dashboard;

import com.bcbs239.regtech.metrics.domain.model.BankId;
import org.springframework.http.MediaType;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.bcbs239.regtech.metrics.presentation.dashboard.dto.DashboardResponse;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.FileItem;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.ComplianceState;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.ReportItem;
import com.bcbs239.regtech.metrics.application.usecase.DashboardUseCase;
import com.bcbs239.regtech.metrics.application.usecase.DashboardResult;
import com.bcbs239.regtech.metrics.domain.model.ComplianceFile;

@Component
public class DashboardController {
        private final DashboardUseCase dashboardUseCase;

        public DashboardController(DashboardUseCase dashboardUseCase) {
                this.dashboardUseCase = dashboardUseCase;
        }

    public RouterFunction<ServerResponse> mapEndpoint() {
        return RouterFunctions.route(
                RequestPredicates.GET("/api/v1/metrics/dashboard"),
                this::getDashboard
        );
    }

    public ServerResponse getDashboard(ServerRequest request) {
                String bankIdParam = request.param("bankId").orElse(null);
                BankId bankId = BankId.of(bankIdParam);


                DashboardResult result = dashboardUseCase.execute(bankId);

        // Fake overlays for score and status (presentation-only)
        List<Double> fakeScores = Arrays.asList(87.2, 94.1, 71.5, 99.0);
        List<String> fakeStatuses = Arrays.asList("VIOLATIONS", "COMPLIANT", "VIOLATIONS", "COMPLIANT");

        List<ComplianceFile> filesDomain = result.files;

        List<FileItem> fileItems = IntStream.range(0, filesDomain.size())
                .mapToObj(i -> new FileItem(
                        filesDomain.get(i).getFilename(),
                        formatDate(filesDomain.get(i).getDate()),
                        fakeScores.get(i % fakeScores.size()),    // FAKE
                        fakeStatuses.get(i % fakeStatuses.size()) // FAKE
                ))
                .collect(Collectors.toList());

        DashboardResponse.Summary summary = new DashboardResponse.Summary(
                result.summary.filesProcessed,
                result.summary.avgScore,
                result.summary.violations,
                result.summary.reports
        );

        // keep existing static compliance and reports placeholders for now
        ComplianceState compliance = new ComplianceState(89.0, 94.0, 87.0, 96.0);
        List<ReportItem> reports = List.of(
                new ReportItem("BCBS_239_Report_Settembre.pdf", "COMPLETED", "Generato: 28 Ago 2025, 09:23 • 1,8 MB • 12 pagine"),
                new ReportItem("Report_Agosto.pdf", "GENERATING", "Generato: 02 Ago 2025, 14:02 • 0,9 MB • 8 pagine")
        );

        Integer lastBatchViolations = result.lastBatchViolations;

        DashboardResponse dashboard = new DashboardResponse(summary, fileItems, compliance, reports, lastBatchViolations);

        Object api = ResponseUtils.success(dashboard);

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(api);
    }

    private String formatDate(String raw) {
        if (raw == null) return null;
        return raw; // simple passthrough for now; formatting can be added later
    }
}
