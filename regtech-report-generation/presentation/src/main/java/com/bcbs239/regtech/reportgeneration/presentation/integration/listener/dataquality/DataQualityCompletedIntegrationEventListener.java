package com.bcbs239.regtech.dataquality.presentation.integration;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.dataquality.application.batch.ProcessBatchCompletedUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("dataQualityBatchCompletedIntegrationEventListener")
public class BatchCompletedIntegrationEventListener {
    private final ProcessBatchCompletedUseCase useCase;

    public BatchCompletedIntegrationEventListener(
            ProcessBatchCompletedUseCase useCase
    ) {
        this.useCase = useCase;
    }

    @EventListener
    public void on(BatchCompletedInboundEvent event) {
        if (CorrelationContext.isInboxReplay()) {
            return;
        }
        ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                .where(CorrelationContext.INBOX_REPLAY, true)
                .run(() -> useCase.process(event));
    }
}

