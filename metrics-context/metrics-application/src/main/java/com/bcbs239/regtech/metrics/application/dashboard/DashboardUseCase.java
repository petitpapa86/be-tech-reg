package com.bcbs239.regtech.metrics.application.dashboard;

import com.bcbs239.regtech.core.application.TimeProvider;
import com.bcbs239.regtech.metrics.application.compliance.port.ComplianceReportRepository;
import com.bcbs239.regtech.metrics.application.dashboard.port.DashboardMetricsRepository;
import com.bcbs239.regtech.metrics.application.dashboard.port.FileRepository;
import com.bcbs239.regtech.metrics.application.signal.ApplicationSignalPublisher;
import com.bcbs239.regtech.metrics.application.signal.DashboardQueriedSignal;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.ComplianceReport;
import com.bcbs239.regtech.metrics.domain.DashboardMetrics;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashboardUseCase {
    private final FileRepository fileRepository;
    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final ComplianceReportRepository complianceReportRepository;
    private final TimeProvider timeProvider;
    private final ApplicationSignalPublisher signalPublisher;

    public DashboardUseCase(
            FileRepository fileRepository,
            DashboardMetricsRepository dashboardMetricsRepository,
            ComplianceReportRepository complianceReportRepository,
            TimeProvider timeProvider,
            ApplicationSignalPublisher signalPublisher
    ) {
        this.fileRepository = fileRepository;
        this.dashboardMetricsRepository = dashboardMetricsRepository;
        this.complianceReportRepository = complianceReportRepository;
        this.timeProvider = timeProvider;
        this.signalPublisher = signalPublisher;
    }

    public DashboardResult execute(BankId bankId) {
        // compute start-of-month and today in ISO format (yyyy-MM-dd)
        java.time.LocalDate now = timeProvider.nowLocalDateTime().toLocalDate();
        java.time.LocalDate startOfMonth = now.withDayOfMonth(1);
        String start = startOfMonth.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        String end = now.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);

        signalPublisher.publish(new DashboardQueriedSignal(bankId == null ? null : bankId.getValue(), start, end));

        List<ComplianceFile> files = fileRepository.findByBankIdAndDateBetween(bankId, start, end);

        List<ComplianceReport> reports = complianceReportRepository.findRecentForMonth(
                bankId,
                startOfMonth,
                now,
                10
        );

        int reportCount = complianceReportRepository.countForMonth(bankId, startOfMonth, now);

        DashboardMetrics metrics = dashboardMetricsRepository.getForMonth(bankId, startOfMonth);

        DashboardResult.Summary summary = new DashboardResult.Summary(
                metrics.getTotalFilesProcessed(),
                metrics.getOverallScore(),
                metrics.getTotalViolations(),
                reportCount
        );

        DashboardResult.Compliance compliance = new DashboardResult.Compliance(
                metrics.getOverallScore(),
                metrics.getDataQualityScore(),
                metrics.getBcbsRulesScore(),
                metrics.getCompletenessScore()
        );

        // Keep behavior minimal for now: last-batch violations derived from the displayed file list.
        return new DashboardResult(summary, compliance, files, reports, computeLastBatchViolations(files));
    }

    private int computeLastBatchViolations(List<ComplianceFile> files) {
        String latestBatch = files.stream()
                .map(ComplianceFile::getBatchId)
                .filter(java.util.Objects::nonNull)
                .max(String::compareTo)
                .orElse(null);

        if (latestBatch == null) {
            return (int) files.stream().filter(f -> !f.isCompliant()).count();
        }

        return (int) files.stream()
                .filter(f -> java.util.Objects.equals(latestBatch, f.getBatchId()))
                .filter(f -> !f.isCompliant())
                .count();
    }
}
