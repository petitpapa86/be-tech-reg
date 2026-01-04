package com.bcbs239.regtech.metrics.application.signal;

import java.time.LocalDate;

public record DashboardMetricsUpdatedSignal(
        String bankId,
        String batchId,
        LocalDate periodStart,
        LocalDate completedDate,
        Double overallScore,
        Double completenessScore,
        Integer totalErrors
) implements ApplicationSignal {
    @Override
    public String type() {
        return "metrics.dashboard.metrics.updated";
    }
}
