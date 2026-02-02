package com.bcbs239.regtech.metrics.application.report;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.metrics.application.compliance.port.ComplianceReportRepository;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListReportsUseCase {

    private final ComplianceReportRepository complianceReportRepository;

    @Observed(name = "usecase.list.reports")
    @Transactional(readOnly = true)
    public Result<ListReportsResponse> execute(ListReportsCommand command) {
        try {
            List<ComplianceReport> reports = complianceReportRepository.findAllWithFilters(
                    command.name(),
                    command.generatedAt(),
                    command.reportingDate(),
                    command.status(),
                    command.page(),
                    command.pageSize()
            );

            int totalCount = complianceReportRepository.countAllWithFilters(
                    command.name(),
                    command.generatedAt(),
                    command.reportingDate(),
                    command.status()
            );

            List<ReportSummary> reportSummaries = reports.stream()
                    .map(this::toReportSummary)
                    .toList();

            ListReportsResponse response = new ListReportsResponse(
                    reportSummaries,
                    new PaginationInfo(
                            command.page(),
                            command.pageSize(),
                            (int) Math.ceil((double) totalCount / command.pageSize()),
                            totalCount,
                            command.page() > 0,
                            command.page() < (int) Math.ceil((double) totalCount / command.pageSize()) - 1
                    ),
                    new FiltersInfo(
                            command.name(),
                            command.generatedAt() != null ? command.generatedAt().toString() : null,
                            command.reportingDate() != null ? command.reportingDate().toString() : null,
                            command.status()
                    )
            );

            return Result.success(response);

        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("LIST_REPORTS_FAILED", ErrorType.SYSTEM_ERROR, "Failed to list reports: " + e.getMessage(), "reports.list.failed"));
        }
    }

    private ReportSummary toReportSummary(ComplianceReport report) {
        return new ReportSummary(
                report.getReportId(),
                report.getReportType(),
                formatFileSize(report.getHtmlFileSize()),
                report.getHtmlPresignedUrl(),
                report.getReportType(),
                report.getStatus(),
                formatDateTime(report.getUpdatedAt()), // Use updatedAt for generatedAt field
                formatPeriod(report.getReportingDate())
        );
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private String formatDateTime(java.time.Instant instant) {
        if (instant == null) return null;
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
    }

    private String formatPeriod(LocalDate reportingDate) {
        if (reportingDate == null) return null;
        return reportingDate.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"));
    }

    public record ListReportsCommand(
            String name,
            LocalDate generatedAt,
            LocalDate reportingDate,
            String status,
            int page,
            int pageSize
    ) {}

    public record ListReportsResponse(
            List<ReportSummary> reports,
            PaginationInfo pagination,
            FiltersInfo filters
    ) {}

    public record ReportSummary(
            String id,
            String name,
            String size,
            String presignedS3Url,
            String reportType,
            String status,
            String generatedAt,
            String period
    ) {}

    public record PaginationInfo(
            int currentPage,
            int pageSize,
            int totalPages,
            int totalItems,
            boolean hasNext,
            boolean hasPrevious
    ) {}

    public record FiltersInfo(
            String name,
            String generatedAt,
            String period,
            String status
    ) {}
}