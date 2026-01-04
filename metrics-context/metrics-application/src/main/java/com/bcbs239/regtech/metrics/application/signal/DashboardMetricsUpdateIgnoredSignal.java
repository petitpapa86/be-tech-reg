package com.bcbs239.regtech.metrics.application.signal;

public record DashboardMetricsUpdateIgnoredSignal(
        String reason,
        String bankId,
        String batchId
) implements ApplicationSignal {
    @Override
    public String type() {
        return "metrics.dashboard.metrics.ignored";
    }

    @Override
    public SignalLevel level() {
        return SignalLevel.WARN;
    }
}
