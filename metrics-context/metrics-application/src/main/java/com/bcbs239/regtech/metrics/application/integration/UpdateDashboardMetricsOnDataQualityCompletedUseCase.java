package com.bcbs239.regtech.metrics.application.integration;

import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;

/**
 * @deprecated Moved to capability package {@code com.bcbs239.regtech.metrics.application.dashboard}.
 * This class remains only to keep older imports compiling.
 */
@Deprecated(forRemoval = true)
public class UpdateDashboardMetricsOnDataQualityCompletedUseCase {

    private final com.bcbs239.regtech.metrics.application.dashboard.UpdateDashboardMetricsOnDataQualityCompletedUseCase delegate;

    public UpdateDashboardMetricsOnDataQualityCompletedUseCase(
            com.bcbs239.regtech.metrics.application.dashboard.UpdateDashboardMetricsOnDataQualityCompletedUseCase delegate
    ) {
        this.delegate = delegate;
    }

    public void process(DataQualityCompletedInboundEvent event) {
        delegate.process(event);
    }
}
