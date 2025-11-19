package com.bcbs239.regtech.ingestion.application.batch.process;

import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedIntegrationEvent;
import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.ingestion.domain.batch.BatchCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Publisher that listens to BatchCompletedEvent domain events and publishes
 * BatchCompletedIntegrationEvent to notify other bounded contexts.
 * 
 * This follows the outbox pattern - the domain event triggers the integration event
 * publication within the same transaction boundary.
 */
@Component
public class BatchCompletedEventPublisher {

    private final IIntegrationEventBus integrationEventBus;
    private static final Logger log = LoggerFactory.getLogger(BatchCompletedEventPublisher.class);

    public BatchCompletedEventPublisher(
            IIntegrationEventBus integrationEventBus) {
        this.integrationEventBus = integrationEventBus;
    }

    /**
     * Handles BatchCompletedEvent domain events and publishes integration events.
     * Uses TransactionalEventListener to ensure the integration event is published
     * within the same transaction as the domain event.
     * 
     * @param event The domain event from the ingestion batch aggregate
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleBatchCompletedEvent(BatchCompletedEvent event) {
        // Skip if this is an outbox replay to avoid duplicate publishing
        if (CorrelationContext.isOutboxReplay()) {
            logEventSkipped(event);
            return;
        }

        try {
            // Create integration event from domain event
            BatchCompletedIntegrationEvent integrationEvent = new BatchCompletedIntegrationEvent(
                    event.batchId().value(),
                    event.bankId().value(),
                    event.s3Reference().uri(),
                    event.totalExposures(),
                    event.fileSizeBytes(),
                    event.completedAt()
            );

            // Publish to other bounded contexts
            integrationEventBus.publish(integrationEvent);

            logEventPublished(event);

        } catch (Exception ex) {
            logEventPublishingError(event, ex);
            throw ex; // Re-throw to ensure transaction rollback
        }
    }

    private void logEventPublished(BatchCompletedEvent event) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "BatchCompletedEvent");
        details.put("batchId", event.batchId().value());
        details.put("bankId", event.bankId().value());
        details.put("totalExposures", event.totalExposures());
        details.put("completedAt", event.completedAt().toString());

        log.info("Published BatchCompletedIntegrationEvent for batch: {} details={}", event.batchId().value(), details);
    }

    private void logEventSkipped(BatchCompletedEvent event) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "BatchCompletedEvent");
        details.put("batchId", event.batchId().value());
        details.put("reason", "Outbox replay - skipping to avoid duplicate");

        log.info("Skipped BatchCompletedIntegrationEvent publishing for batch: {} details={}", event.batchId().value(), details);
    }

    private void logEventPublishingError(BatchCompletedEvent event, Exception ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "BatchCompletedEvent");
        details.put("batchId", event.batchId().value());
        details.put("bankId", event.bankId().value());
        details.put("errorMessage", ex.getMessage());

        log.error("Failed to publish BatchCompletedIntegrationEvent for batch: {} details={}", event.batchId().value(), details, ex);
    }
}
