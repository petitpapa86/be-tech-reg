package com.bcbs239.regtech.metrics.infrastructure.repository;

import com.bcbs239.regtech.metrics.application.port.DashboardMetricsRepository;
import com.bcbs239.regtech.metrics.domain.BankId;
import com.bcbs239.regtech.metrics.domain.DashboardMetrics;
import com.bcbs239.regtech.metrics.infrastructure.entity.DashboardMetricsEntity;
import com.bcbs239.regtech.metrics.infrastructure.entity.DashboardMetricsKey;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class JpaDashboardMetricsRepository implements DashboardMetricsRepository {

    private final SpringDataDashboardMetricsRepository repo;

    public JpaDashboardMetricsRepository(SpringDataDashboardMetricsRepository repo) {
        this.repo = repo;
    }

    @Override
    public DashboardMetrics getForMonth(BankId bankId, LocalDate periodStart) {
        if (bankId == null) {
            throw new IllegalArgumentException("bankId must not be null");
        }
        if (periodStart == null) {
            throw new IllegalArgumentException("periodStart must not be null");
        }

        String bank = bankId.getValue();
        DashboardMetricsEntity entity = repo.findByKeyBankIdAndKeyPeriodStart(bank, periodStart)
                .orElseGet(() -> {
                    DashboardMetricsEntity created = new DashboardMetricsEntity();
                    created.setKey(new DashboardMetricsKey(bank, periodStart));
                    return repo.save(created);
                });

        return toDomain(entity, bankId);
    }

    @Override
    public DashboardMetrics save(DashboardMetrics metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("metrics must not be null");
        }

        DashboardMetricsEntity entity = toEntity(metrics);
        DashboardMetricsEntity saved = repo.save(entity);
        return toDomain(saved, metrics.getBankId());
    }

    private DashboardMetrics toDomain(DashboardMetricsEntity e, BankId bankId) {
        LocalDate periodStart = e.getKey() == null ? null : e.getKey().getPeriodStart();
        return new DashboardMetrics(
                bankId,
                periodStart,
                e.getOverallScore(),
                e.getDataQualityScore(),
                e.getBcbsRulesScore(),
                e.getCompletenessScore(),
                e.getTotalFilesProcessed(),
                e.getTotalViolations(),
                e.getTotalReportsGenerated(),
                e.getTotalExposures(),
                e.getValidExposures(),
                e.getTotalErrors(),
                e.getVersion()
        );
    }

    private DashboardMetricsEntity toEntity(DashboardMetrics m) {
        DashboardMetricsEntity e = new DashboardMetricsEntity();

        if (m.getBankId() == null || m.getBankId().getValue() == null) {
            throw new IllegalArgumentException("metrics.bankId must not be null");
        }
        if (m.getPeriodStart() == null) {
            throw new IllegalArgumentException("metrics.periodStart must not be null");
        }

        e.setKey(new DashboardMetricsKey(m.getBankId().getValue(), m.getPeriodStart()));
        e.setOverallScore(m.getOverallScore());
        e.setDataQualityScore(m.getDataQualityScore());
        e.setBcbsRulesScore(m.getBcbsRulesScore());
        e.setCompletenessScore(m.getCompletenessScore());
        e.setTotalFilesProcessed(m.getTotalFilesProcessed());
        e.setTotalViolations(m.getTotalViolations());
        e.setTotalReportsGenerated(m.getTotalReportsGenerated());
        e.setTotalExposures(m.getTotalExposures());
        e.setValidExposures(m.getValidExposures());
        e.setTotalErrors(m.getTotalErrors());
        e.setVersion(m.getVersion());
        return e;
    }
}
