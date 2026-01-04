package com.bcbs239.regtech.metrics.presentation.integration.listener;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;
import com.bcbs239.regtech.metrics.application.dashboard.UpdateDashboardMetricsOnDataQualityCompletedUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("metricsDataQualityCompletedIntegrationEventListener")
public class DataQualityCompletedIntegrationEventListener {

    private final UpdateDashboardMetricsOnDataQualityCompletedUseCase useCase;

    public DataQualityCompletedIntegrationEventListener(UpdateDashboardMetricsOnDataQualityCompletedUseCase useCase) {
        this.useCase = useCase;
    }

    @EventListener
    public void on(DataQualityCompletedInboundEvent event) {
        if (CorrelationContext.isInboxReplay()) {
            return;
        }
        ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                .where(CorrelationContext.INBOX_REPLAY, true)
                .run(() -> useCase.process(event));
    }
}
