package com.bcbs239.regtech.reportgeneration.presentation.integration.listener.riskcalculation;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.core.domain.events.integration.RiskCalculationCompletedInboundEvent;
import com.bcbs239.regtech.reportgeneration.application.ingestionbatch.ProcessRiskCalculationCompletedUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("riskCalculationCompletedIntegrationEventListener")
public class RiskCalculationCompletedIntegrationEventListener {
    private final ProcessRiskCalculationCompletedUseCase useCase;

    public RiskCalculationCompletedIntegrationEventListener(
            ProcessRiskCalculationCompletedUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @EventListener
    public void on(RiskCalculationCompletedInboundEvent event) {
        if (CorrelationContext.isInboxReplay()) {
            return;
        }
        ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                .where(CorrelationContext.INBOX_REPLAY, true)
                .run(() -> useCase.process(event));
    }
}