package com.bcbs239.regtech.metrics.application.usecase;

import com.bcbs239.regtech.metrics.application.port.FileRepository;
import com.bcbs239.regtech.metrics.domain.model.ComplianceFile;
import com.bcbs239.regtech.metrics.domain.service.DashboardMetrics;
import com.bcbs239.regtech.metrics.domain.model.BankId;
import com.bcbs239.regtech.core.application.TimeProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashboardUseCase {
    private final FileRepository fileRepository;
    private final TimeProvider timeProvider;

    public DashboardUseCase(FileRepository fileRepository, TimeProvider timeProvider) {
        this.fileRepository = fileRepository;
        this.timeProvider = timeProvider;
    }

    public DashboardResult execute(BankId bankId) {
        // compute start-of-month and today in ISO format (yyyy-MM-dd)
        java.time.LocalDate now = timeProvider.nowLocalDateTime().toLocalDate();
        java.time.LocalDate startOfMonth = now.withDayOfMonth(1);
        String start = startOfMonth.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        String end = now.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);

        List<ComplianceFile> files = fileRepository.findByBankIdAndDateBetween(bankId, start, end);


        // Delegate business calculations to domain (month-to-date)
        DashboardMetrics.Result metrics = DashboardMetrics.compute(files);

        DashboardResult.Summary summary = new DashboardResult.Summary(metrics.filesProcessed(), metrics.avgScore(), metrics.violations(), metrics.reports());

        return new DashboardResult(summary, files, metrics.lastBatchViolations());
    }
}
