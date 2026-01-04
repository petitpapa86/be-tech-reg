package com.bcbs239.regtech.metrics.application.integration;

import com.bcbs239.regtech.core.domain.events.integration.ComplianceReportGeneratedInboundEvent;

/**
 * @deprecated Moved to capability package {@code com.bcbs239.regtech.metrics.application.compliance}.
 * This class remains only to keep older imports compiling.
 */
@Deprecated(forRemoval = true)
public class UpsertComplianceReportOnReportGeneratedUseCase {

    private final com.bcbs239.regtech.metrics.application.compliance.UpsertComplianceReportOnReportGeneratedUseCase delegate;

    public UpsertComplianceReportOnReportGeneratedUseCase(
            com.bcbs239.regtech.metrics.application.compliance.UpsertComplianceReportOnReportGeneratedUseCase delegate
    ) {
        this.delegate = delegate;
    }

    public void process(ComplianceReportGeneratedInboundEvent event) {
        delegate.process(event);
    }
}
