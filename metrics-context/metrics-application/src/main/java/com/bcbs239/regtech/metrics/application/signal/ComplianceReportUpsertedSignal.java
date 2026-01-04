package com.bcbs239.regtech.metrics.application.signal;

public record ComplianceReportUpsertedSignal(
        String reportId,
        String bankId,
        String status,
        String reportType,
        String reportingDate
) implements ApplicationSignal {
    @Override
    public String type() {
        return "metrics.compliance.report.upserted";
    }
}
