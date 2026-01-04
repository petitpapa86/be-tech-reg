package com.bcbs239.regtech.metrics.application.usecase;

import com.bcbs239.regtech.metrics.domain.BankId;

/**
 * @deprecated Moved to capability package {@code com.bcbs239.regtech.metrics.application.dashboard}.
 * This class remains only to keep older imports compiling.
 */
@Deprecated(forRemoval = true)
public class DashboardUseCase {

    private final com.bcbs239.regtech.metrics.application.dashboard.DashboardUseCase delegate;

    public DashboardUseCase(com.bcbs239.regtech.metrics.application.dashboard.DashboardUseCase delegate) {
        this.delegate = delegate;
    }

    public DashboardResult execute(BankId bankId) {
        com.bcbs239.regtech.metrics.application.dashboard.DashboardResult r = delegate.execute(bankId);

        DashboardResult.Summary summary = new DashboardResult.Summary(
                r.summary.filesProcessed,
                r.summary.avgScore,
                r.summary.violations,
                r.summary.reports
        );

        DashboardResult.Compliance compliance = r.compliance == null
                ? null
                : new DashboardResult.Compliance(
                r.compliance.overall,
                r.compliance.dataQuality,
                r.compliance.bcbs,
                r.compliance.completeness
        );

        return new DashboardResult(summary, compliance, r.files, r.reports, r.lastBatchViolations);
    }
}
