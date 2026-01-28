package com.bcbs239.regtech.metrics.presentation.dashboard;

import com.bcbs239.regtech.metrics.domain.BankId;
import org.springframework.http.MediaType;
import com.bcbs239.regtech.core.presentation.apiresponses.ResponseUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.bcbs239.regtech.metrics.presentation.dashboard.dto.DashboardResponse;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.FileItem;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.ComplianceState;
import com.bcbs239.regtech.metrics.presentation.dashboard.dto.ReportItem;
import com.bcbs239.regtech.metrics.application.dashboard.DashboardUseCase;
import com.bcbs239.regtech.metrics.application.dashboard.DashboardResult;
import com.bcbs239.regtech.metrics.domain.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;

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

        int page = request.param("page").map(Integer::parseInt).orElse(0);
        int size = request.param("size").map(Integer::parseInt).orElse(10);

        DashboardResult result = dashboardUseCase.execute(bankId, page, size);

        List<ComplianceFile> filesDomain = result.files;

        List<FileItem> fileItems = filesDomain.stream()
                .map(file -> new FileItem(
                        file.getId(),
                        file.getFilename(),
                        formatDate(file.getDate()),
                        file.getScore(),
                        file.getStatus(),
                        file.getReportId() != null ? file.getReportId().value() : null
                ))
                .toList();

        DashboardResponse.Summary summary = new DashboardResponse.Summary(
                result.summary.filesProcessed,
                result.summary.avgScore,
                result.summary.violations,
                result.summary.reports
        );

        ComplianceState compliance = new ComplianceState(
                result.compliance != null ? result.compliance.overall : null,
                result.compliance != null ? result.compliance.dataQuality : null,
                result.compliance != null ? result.compliance.bcbs : null,
                result.compliance != null ? result.compliance.completeness : null
        );
        List<ReportItem> reports = result.reports.stream()
                .map(this::toReportItem)
                .toList();

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

    private ReportItem toReportItem(ComplianceReport report) {
        String filename = firstNonBlank(
                extractFileNameFromS3Uri(report.getHtmlS3Uri()),
                extractFileNameFromS3Uri(report.getXbrlS3Uri()),
                report.getReportType() + "_" + report.getReportingDate() + "_" + report.getReportId()
        );

        Long htmlSize = java.util.Objects.requireNonNullElse(report.getHtmlFileSize(), 0L);
        Long xbrlSize = java.util.Objects.requireNonNullElse(report.getXbrlFileSize(), 0L);
        long totalSize = htmlSize + xbrlSize;

        String generatedAt = report.getGeneratedAt() == null
                ? "unknown"
                : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(report.getGeneratedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());

        String details = "Generated: " + generatedAt
                + " • Size: " + formatBytes(totalSize)
                + (report.getComplianceStatus() != null ? " • Compliance: " + report.getComplianceStatus() : "")
                + (report.getOverallQualityScore() != null ? " • Score: " + report.getOverallQualityScore() : "");

        return new ReportItem(filename, report.getStatus(), details);
    }

    private String extractFileNameFromS3Uri(String s3Uri) {
        if (s3Uri == null || s3Uri.isBlank()) {
            return null;
        }
        int lastSlash = s3Uri.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == s3Uri.length() - 1) {
            return null;
        }
        return s3Uri.substring(lastSlash + 1);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(java.util.Locale.ROOT, "%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format(java.util.Locale.ROOT, "%.1f GB", gb);
    }
}
