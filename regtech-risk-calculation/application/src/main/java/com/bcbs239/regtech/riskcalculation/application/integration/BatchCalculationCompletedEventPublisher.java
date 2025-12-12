package com.bcbs239.regtech.riskcalculation.application.integration;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.BatchCalculationCompletedIntegrationEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component("riskCalculationBatchCalculationCompletedEventPublisher")
public class BatchCalculationCompletedEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BatchCalculationCompletedEventPublisher.class);

    private final IIntegrationEventBus eventBus;

    public BatchCalculationCompletedEventPublisher(IIntegrationEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @EventListener
    public void handle(BatchCalculationCompletedEvent event) {
        if (CorrelationContext.isOutboxReplay()) {
            logger.debug("Skipping integration publish for BatchCalculationCompletedEvent {} because this is an outbox replay", event.getEventId());
            return;
        }
        try {
            logger.info("Converting and publishing BatchCalculationCompletedIntegrationEvent for batch {}", event.getBatchId());

            BatchCalculationCompletedIntegrationEvent integrationEvent = new BatchCalculationCompletedIntegrationEvent(
                    event.getBatchId(),
                    event.getBankId(),
                    event.getCalculationResultsUri(),
                    event.getCompletedAt(),
                    event.getTotalAmountEur(),
                    event.getProcessedExposures()
            );
            ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                    // .where(CorrelationContext.CAUSATION_ID, event.getCausationId().getValue())
                    //.where(CorrelationContext.BOUNDED_CONTEXT, event.getBoundedContext())
                    .where(CorrelationContext.OUTBOX_REPLAY, true)
                    .where(CorrelationContext.INBOX_REPLAY, false)
                    .run(() -> {
                        eventBus.publish(integrationEvent);
                    });
            logger.info("Published BatchCalculationCompletedIntegrationEvent for batch {}", event.getBatchId());

        } catch (Exception ex) {
            logger.error("Failed to publish BatchCalculationCompletedIntegrationEvent for batch {}", event.getBatchId(), ex);
            throw ex;
        }
    }
}