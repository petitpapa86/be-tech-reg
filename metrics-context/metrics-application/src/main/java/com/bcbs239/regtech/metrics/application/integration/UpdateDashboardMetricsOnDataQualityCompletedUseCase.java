package com.bcbs239.regtech.metrics.application.integration;

import com.bcbs239.regtech.core.application.TimeProvider;
import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;
import com.bcbs239.regtech.metrics.application.port.DashboardMetricsRepository;
import com.bcbs239.regtech.metrics.application.port.FileRepository;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.DashboardMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class UpdateDashboardMetricsOnDataQualityCompletedUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateDashboardMetricsOnDataQualityCompletedUseCase.class);

    private final DashboardMetricsRepository dashboardMetricsRepository;
    private final FileRepository fileRepository;
    private final TimeProvider timeProvider;

    public UpdateDashboardMetricsOnDataQualityCompletedUseCase(
            DashboardMetricsRepository dashboardMetricsRepository,
            FileRepository fileRepository,
            TimeProvider timeProvider
    ) {
        this.dashboardMetricsRepository = dashboardMetricsRepository;
        this.fileRepository = fileRepository;
        this.timeProvider = timeProvider;
    }

    public void process(DataQualityCompletedInboundEvent event) {
        if (event == null) {
            log.warn("Metrics: received null DataQualityCompletedInboundEvent");
            return;
        }
        if (!event.isValid()) {
            log.warn("Metrics: invalid DataQualityCompletedInboundEvent, skipping. batchId={}, bankId={} ", event.getBatchId(), event.getBankId());
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

        DashboardMetrics metrics = dashboardMetricsRepository.getForMonth(bankId, periodStart);
        metrics.recalculateMonthToDate(files);

        // Persist data-quality scores + counts when provided by the event
        metrics.onDataQualityCompleted(
            event.getOverallScore(),
            event.getCompletenessScore(),
            event.getTotalExposures(),
            event.getValidExposures(),
            event.getTotalErrors()
        );

        dashboardMetricsRepository.save(metrics);
    }
}
