package com.bcbs239.regtech.metrics.application.dashboard;

import com.bcbs239.regtech.core.application.TimeProvider;
import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;
import com.bcbs239.regtech.metrics.application.dashboard.port.DashboardMetricsRepository;
import com.bcbs239.regtech.metrics.application.dashboard.port.FileRepository;
import com.bcbs239.regtech.metrics.application.signal.ApplicationSignalPublisher;
import com.bcbs239.regtech.metrics.application.signal.DashboardMetricsUpdateIgnoredSignal;
import com.bcbs239.regtech.metrics.application.signal.DashboardMetricsUpdatedSignal;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.DashboardMetrics;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class UpdateDashboardMetricsOnDataQualityCompletedUseCase {

    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final FileRepository fileRepository;
    private final TimeProvider timeProvider;
    private final ApplicationSignalPublisher signalPublisher;

    public UpdateDashboardMetricsOnDataQualityCompletedUseCase(
            DashboardMetricsRepository dashboardMetricsRepository,
            FileRepository fileRepository,
            TimeProvider timeProvider,
            ApplicationSignalPublisher signalPublisher
    ) {
        this.dashboardMetricsRepository = dashboardMetricsRepository;
        this.fileRepository = fileRepository;
        this.timeProvider = timeProvider;
        this.signalPublisher = signalPublisher;
    }

    public void process(DataQualityCompletedInboundEvent event) {
        if (event == null) {
            signalPublisher.publish(new DashboardMetricsUpdateIgnoredSignal("null_event", null, null));
            return;
        }
        if (!event.isValid()) {
            signalPublisher.publish(new DashboardMetricsUpdateIgnoredSignal("invalid_event", event.getBankId(), event.getBatchId()));
            return;
        }

        BankId bankId = BankId.of(event.getBankId());

        LocalDate completedDate = event.getCompletedAt() != null
                ? LocalDate.ofInstant(event.getCompletedAt(), ZoneId.systemDefault())
                : timeProvider.nowLocalDateTime().toLocalDate();

        LocalDate periodStart = completedDate.withDayOfMonth(1);

        String start = periodStart.toString();
        String end = completedDate.toString();

        List<ComplianceFile> files = fileRepository.findByBankIdAndDateBetween(bankId, start, end);

        // Load metrics entity
        DashboardMetrics metrics = dashboardMetricsRepository.getForMonth(bankId, periodStart);

        // Save file first (including completenessScore)
        ComplianceFile file = new ComplianceFile(
            event.getFilename(),
            completedDate.toString(),
            event.getOverallScore(),
            event.getCompletenessScore(),
            event.getComplianceStatus() ? "COMPLIANT" : "NON_COMPLIANT",
            event.getBatchId(),
            bankId
        );
        fileRepository.save(file);

        // Merge samples into persisted TDigest sketches and get updated medians
        metrics = dashboardMetricsRepository.addSamplesAndGet(bankId, periodStart, event.getOverallScore(), event.getCompletenessScore());

        // Recompute overall month-to-date from persisted files
        metrics.recalculateMonthToDate(files);

        // Update counts
        metrics.onDataQualityCompleted(
            metrics.getDataQualityScore(),
            metrics.getCompletenessScore(),
            event.getTotalExposures(),
            event.getValidExposures(),
            event.getTotalErrors()
        );

        dashboardMetricsRepository.save(metrics);
        
        signalPublisher.publish(new DashboardMetricsUpdatedSignal(
                event.getBankId(),
                event.getBatchId(),
                periodStart,
                completedDate,
                event.getOverallScore(),
                event.getCompletenessScore(),
                event.getTotalErrors()
        ));
    }
}
