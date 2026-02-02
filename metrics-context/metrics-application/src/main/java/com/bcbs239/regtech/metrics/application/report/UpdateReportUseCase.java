package com.bcbs239.regtech.metrics.application.report;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.metrics.application.compliance.port.ComplianceReportRepository;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;
import com.bcbs239.regtech.metrics.domain.ReportStatus;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateReportUseCase {

    private final ComplianceReportRepository complianceReportRepository;

    @Observed(name = "usecase.update.report")
    @Transactional
    public Result<UpdateReportResponse> execute(UpdateReportCommand command) {
        try {
            // Find the existing report
            ComplianceReport existingReport = complianceReportRepository.findById(command.reportId())
                    .orElseThrow(() -> new IllegalArgumentException("Report not found: " + command.reportId()));

            // Validate status
            ReportStatus newStatus = ReportStatus.fromString(command.status());
            if (newStatus == null) {
                return Result.failure(ErrorDetail.of("INVALID_STATUS", ErrorType.VALIDATION_ERROR,
                    "Invalid status: " + command.status(), "report.status.invalid"));
            }

            // Update the report
            ComplianceReport updatedReport = existingReport.updateStatus(newStatus);
            ComplianceReport savedReport = complianceReportRepository.save(updatedReport);

            UpdateReportResponse response = new UpdateReportResponse(
                    savedReport.getReportId(),
                    savedReport.getReportType(),
                    formatFileSize(savedReport.getHtmlFileSize()),
                    savedReport.getHtmlPresignedUrl(),
                    savedReport.getReportType(),
                    savedReport.getStatus(),
                    formatDateTime(savedReport.getUpdatedAt()), // Use updatedAt for generatedAt field
                    formatPeriod(savedReport.getReportingDate())
            );

            return Result.success(response);

        } catch (IllegalArgumentException e) {
            return Result.failure(ErrorDetail.of("REPORT_NOT_FOUND", ErrorType.NOT_FOUND_ERROR,
                e.getMessage(), "report.not.found"));
        } catch (Exception e) {
            return Result.failure(ErrorDetail.of("UPDATE_REPORT_FAILED", ErrorType.SYSTEM_ERROR,
                "Failed to update report: " + e.getMessage(), "report.update.failed"));
        }
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

    private String formatPeriod(java.time.LocalDate reportingDate) {
        if (reportingDate == null) return null;
        return reportingDate.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"));
    }

    public record UpdateReportCommand(
            String reportId,
            String status
    ) {}

    public record UpdateReportResponse(
            String id,
            String name,
            String size,
            String presignedS3Url,
            String reportType,
            String status,
            String generatedAt,
            String period
    ) {}
}