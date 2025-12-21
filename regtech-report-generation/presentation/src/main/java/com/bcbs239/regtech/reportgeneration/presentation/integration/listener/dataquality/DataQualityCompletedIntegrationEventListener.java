package com.bcbs239.regtech.reportgeneration.presentation.integration.listener.dataquality;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;
import com.bcbs239.regtech.reportgeneration.application.ingestionbatch.ProcessDataQualityCompletedUseCase;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("dataQualityBatchCompletedIntegrationEventListener")
public class DataQualityCompletedIntegrationEventListener {
    private final ProcessDataQualityCompletedUseCase useCase;

    public DataQualityCompletedIntegrationEventListener(
            ProcessDataQualityCompletedUseCase useCase
    ) {
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

