package com.bcbs239.regtech.metrics.presentation.integration.listener;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.ComplianceReportGeneratedInboundEvent;
import com.bcbs239.regtech.metrics.application.compliance.UpsertComplianceReportOnReportGeneratedUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("metricsComplianceReportGeneratedIntegrationEventListener")
public class ComplianceReportGeneratedIntegrationEventListener {

    private final UpsertComplianceReportOnReportGeneratedUseCase useCase;

    public ComplianceReportGeneratedIntegrationEventListener(UpsertComplianceReportOnReportGeneratedUseCase useCase) {
        this.useCase = useCase;
    }

    @EventListener
    public void on(ComplianceReportGeneratedInboundEvent event) {
        if (CorrelationContext.isInboxReplay()) {
            return;
        }

        ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                .where(CorrelationContext.INBOX_REPLAY, true)
                .run(() -> useCase.process(event));
    }
}
