package com.bcbs239.regtech.riskcalculation.application.integration;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedIntegrationEvent;
import com.bcbs239.regtech.riskcalculation.application.integration.events.BatchIngestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adapter that converts shared BatchCompletedIntegrationEvent from the inbox
 * into processing logic for the risk-calculation bounded context.
 * 
 * This adapter listens for batch completion events and triggers
 * risk calculation workflows such as:
 * - Portfolio risk metrics calculation
 * - Concentration indices computation
 * - Exposure classification and valuation
 * - Risk aggregation and analysis
 * 
 * This allows the risk-calculation module to:
 * - React to batch lifecycle events
 * - Keep its internal domain model decoupled from other modules
 * - Process events through its own domain event handlers
 * - Avoid circular dependencies on integration events
 */
@Component("riskCalculationBatchCompletedIntegrationAdapter")
public class BatchCompletedIntegrationAdapter {

    private final ApplicationEventPublisher domainEventBus;
    private static final Logger log = LoggerFactory.getLogger(BatchCompletedIntegrationAdapter.class);

    public BatchCompletedIntegrationAdapter(ApplicationEventPublisher domainEventBus) {
        this.domainEventBus = domainEventBus;
        log.info("✅ BatchCompletedIntegrationAdapter bean created successfully!");
    }

    /**
     * Handles BatchCompletedIntegrationEvent from the integration event bus.
     * This event is published when a batch has completed processing in the ingestion module.
     * 
     * Converts the BatchCompletedIntegrationEvent to a BatchIngestedEvent and publishes it
     * as a replay to trigger risk calculation workflows in the risk-calculation module.
     * 
     * The listener checks for outbox replays and skips duplicate processing.
     * 
     * @param integrationEvent The batch completed integration event from the ingestion module
     */
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener
    public void onBatchCompletedIntegrationEvent(BatchCompletedIntegrationEvent integrationEvent) {
        if (CorrelationContext.isInboxReplay()) {
            return;
        }
        log.info("🔔 BatchCompletedIntegrationAdapter received event; details={}", Map.of(
            "eventType", "BATCH_COMPLETED_INTEGRATION_EVENT",
            "batchId", integrationEvent.getBatchId(),
            "bankId", integrationEvent.getBankId()
        ));

        // Convert BatchCompletedIntegrationEvent to BatchIngestedEvent
        // This allows the risk-calculation module to process completed batches
        // using its existing BatchIngestedEventListener
        BatchIngestedEvent batchIngestedEvent = new BatchIngestedEvent(
            integrationEvent.getBatchId(),
            integrationEvent.getBankId(),
            integrationEvent.getS3Uri(),
            integrationEvent.getTotalExposures(),
            integrationEvent.getFileSizeBytes(),
            integrationEvent.getCompletedAt()
        );
        batchIngestedEvent.setCorrelationId(integrationEvent.getCorrelationId());
        batchIngestedEvent.setCausationId(integrationEvent.getCausationId());

        // Publish as replay so listener can detect and skip if it's a duplicate
        domainEventBus.publishEvent(batchIngestedEvent);
        
        log.info("Published BatchIngestedEvent as replay for risk-calculation processing; details={}", Map.of(
            "eventId", batchIngestedEvent.getEventId(),
            "batchId", integrationEvent.getBatchId()
        ));
    }
}
