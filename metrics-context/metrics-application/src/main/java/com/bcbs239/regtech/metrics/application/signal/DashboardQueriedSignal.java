package com.bcbs239.regtech.metrics.application.signal;

public record DashboardQueriedSignal(
        String bankId,
        String startDate,
        String endDate
) implements ApplicationSignal {
    @Override
    public String type() {
        return "metrics.dashboard.queried";
    }
}
