package com.bcbs239.regtech.dataquality.application.integration;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.BatchQualityCompletedIntegrationEvent;
import com.bcbs239.regtech.dataquality.domain.report.events.QualityValidationCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component("dataQualityBatchQualityCompletedEventPublisher")
public class BatchQualityCompletedEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BatchQualityCompletedEventPublisher.class);

    private final IIntegrationEventBus eventBus;

    @Autowired
    public BatchQualityCompletedEventPublisher(IIntegrationEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @EventListener
    public void handle(QualityValidationCompletedEvent event) {
        if (CorrelationContext.isOutboxReplay()) {
            logger.debug("Skipping integration publish for QualityValidationCompletedEvent {} because this is an outbox replay", event.getEventId());
            return;
        }
        try {
            logger.info("Converting and publishing BatchQualityCompletedIntegrationEvent for batch {}", event.getBatchId().value());

            BatchQualityCompletedIntegrationEvent integrationEvent = new BatchQualityCompletedIntegrationEvent(
                    event.getBatchId().value(),
                    event.getBankId().value(),
                    event.getDetailsReference().uri(),
                    event.getQualityScores().overallScore(),
                    event.getQualityGrade().getLetterGrade()
            );

            ScopedValue.where(CorrelationContext.CORRELATION_ID, event.getCorrelationId())
                    // .where(CorrelationContext.CAUSATION_ID, event.getCausationId().getValue())
                    //.where(CorrelationContext.BOUNDED_CONTEXT, event.getBoundedContext())
                    .where(CorrelationContext.OUTBOX_REPLAY, true)
                    .where(CorrelationContext.INBOX_REPLAY, false)
                    .run(() -> {
                        eventBus.publish(integrationEvent);
                    });

            logger.info("Published BatchQualityCompletedIntegrationEvent for batch {}", event.getBatchId().value());

        } catch (Exception ex) {
            logger.error("Failed to publish BatchQualityCompletedIntegrationEvent for batch {}", event.getBatchId().value(), ex);
            throw ex;
        }
    }
}