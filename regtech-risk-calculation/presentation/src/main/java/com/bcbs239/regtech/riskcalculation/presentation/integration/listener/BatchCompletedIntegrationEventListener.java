package com.bcbs239.regtech.riskcalculation.presentation.batch;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.riskcalculation.application.batch.ProcessBatchCompletedUseCase;

import lombok.extern.slf4j.Slf4j;

@Component("riskCalculationBatchCompletedIntegrationEventListener")
@Slf4j
public class BatchCompletedIntegrationEventListener {
    private final ProcessBatchCompletedUseCase useCase;

    public BatchCompletedIntegrationEventListener(
            ProcessBatchCompletedUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @EventListener
    public void on(BatchCompletedInboundEvent event) {
        log.info("\uD83D\uDCE6 RiskCalculation received BatchCompletedInboundEvent: batchId={}, bankId={}, totalExposures={}, isInboxReplay={}, isOutboxReplay={}",
                event.getBatchId(), event.getBankId(), event.getTotalExposures(),
                CorrelationContext.isInboxReplay(), CorrelationContext.isOutboxReplay());
        if (CorrelationContext.isInboxReplay()) {
            return;
        }
        ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                .where(CorrelationContext.INBOX_REPLAY, true)
                .run(() -> useCase.process(event));
    }
}

