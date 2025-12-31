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

@Component
public class DashboardController {

    public RouterFunction<ServerResponse> mapEndpoint() {
        return RouterFunctions.route(
                RequestPredicates.GET("/api/v1/metrics/dashboard"),
                this::getDashboard
        );
    }

        public ServerResponse getDashboard(ServerRequest request) {
                FakeSummary summary = new FakeSummary(35, 95.0, 85, 155);

                List<FakeFile> files = List.of(
                        new FakeFile("esposizioni_settembre.xlsx", "15 Set 2028", 87.2, "VIOLATIONS"),
                        new FakeFile("grandi_esposizioni_agosto.xlsx", "28 Ago 2029", 94.1, "COMPLIANT")
                );

                FakeCompliance compliance = new FakeCompliance(99.0, 89.0, 79.0, 69.0);

                List<FakeReport> reports = List.of(
                        new FakeReport("Report_GUINGNO.pdf", "COMPLETED", "Generato: 28 Ago 2025, 09:23 • 1,8 MB • 2 pagine"),
                        new FakeReport("Report_Agosto.pdf", "GENERATING", "Generato: 02 Ago 2025, 14:02 • 0,9 MB • 10 pagine")
                );

                FakeLastBatchViolations lastBatchViolations = new FakeLastBatchViolations(12);

                FakeDashboard dashboard = new FakeDashboard(summary, files, compliance, reports, lastBatchViolations);

                Object api = ResponseUtils.success(dashboard);

                return ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(api);
        }

        // Temporary POJOs for Java 8 compatibility
        public static class FakeDashboard {
                public final FakeSummary summary;
                public final List<FakeFile> files;
                public final FakeCompliance compliance;
                public final List<FakeReport> reports;
                public final FakeLastBatchViolations lastBatchViolations;

                public FakeDashboard(FakeSummary summary, List<FakeFile> files, FakeCompliance compliance, List<FakeReport> reports, FakeLastBatchViolations lastBatchViolations) {
                        this.summary = summary;
                        this.files = files;
                        this.compliance = compliance;
                        this.reports = reports;
                        this.lastBatchViolations = lastBatchViolations;
                }
        }

        public static class FakeLastBatchViolations {
                public final Integer count;

                public FakeLastBatchViolations(Integer count) {
                        this.count = count;
                }
        }

        public static class FakeSummary {
                public final Integer filesProcessed;
                public final Double avgScore;
                public final Integer violations;
                public final Integer reports;

                public FakeSummary(Integer filesProcessed, Double avgScore, Integer violations, Integer reports) {
                        this.filesProcessed = filesProcessed;
                        this.avgScore = avgScore;
                        this.violations = violations;
                        this.reports = reports;
                }
        }

        public static class FakeFile {
                public final String filename;
                public final String date;
                public final Double score;
                public final String status;

                public FakeFile(String filename, String date, Double score, String status) {
                        this.filename = filename;
                        this.date = date;
                        this.score = score;
                        this.status = status;
                }
        }

        public static class FakeCompliance {
                public final Double overall;
                public final Double dataQuality;
                public final Double bcbs;
                public final Double completeness;

                public FakeCompliance(Double overall, Double dataQuality, Double bcbs, Double completeness) {
                        this.overall = overall;
                        this.dataQuality = dataQuality;
                        this.bcbs = bcbs;
                        this.completeness = completeness;
                }
        }

        public static class FakeReport {
                public final String filename;
                public final String status;
                public final String details;

                public FakeReport(String filename, String status, String details) {
                        this.filename = filename;
                        this.status = status;
                        this.details = details;
                }
        }
}
