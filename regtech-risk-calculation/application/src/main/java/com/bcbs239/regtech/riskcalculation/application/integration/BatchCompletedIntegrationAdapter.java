package com.bcbs239.regtech.riskcalculation.application.integration;

import com.bcbs239.regtech.core.domain.events.DomainEventBus;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedIntegrationEvent;
import com.bcbs239.regtech.ingestion.domain.integrationevents.BatchIngestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final DomainEventBus domainEventBus;
    private static final Logger log = LoggerFactory.getLogger(BatchCompletedIntegrationAdapter.class);

    public BatchCompletedIntegrationAdapter(DomainEventBus domainEventBus) {
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
     * @param integrationEvent The batch completed integration event from the ingestion module
     */
    @EventListener
    public void onBatchCompletedIntegrationEvent(BatchCompletedIntegrationEvent integrationEvent) {
        try {
            log.info("🔔 BatchCompletedIntegrationAdapter invoked! details={}", Map.of(
                "eventType", "BATCH_COMPLETED_INTEGRATION_EVENT",
                "integrationEventId", integrationEvent.getEventId(),
                "batchId", integrationEvent.getBatchId(),
                "bankId", integrationEvent.getBankId(),
                "s3Uri", integrationEvent.getS3Uri(),
                "totalExposures", integrationEvent.getTotalExposures(),
                "fileSizeBytes", integrationEvent.getFileSizeBytes(),
                "completedAt", integrationEvent.getCompletedAt().toString(),
                "correlationId", integrationEvent.getCorrelationId(),
                "isInboxReplay", String.valueOf(com.bcbs239.regtech.core.domain.context.CorrelationContext.isInboxReplay())
            ));

            // Skip processing entirely if this is an inbox replay
            // Events are processed once during initial dispatch, inbox replay is for reliability only
            if (com.bcbs239.regtech.core.domain.context.CorrelationContext.isInboxReplay()) {
                log.info("Skipping inbox replay for BatchCompletedIntegrationEvent: {}", integrationEvent.getBatchId());
                return;
            }

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

            // Publish as replay so existing risk-calculation handlers receive it
            // This will trigger the BatchIngestedEventListener to process the batch
            domainEventBus.publishAsReplay(batchIngestedEvent);
            
            log.info("Published BatchIngestedEvent as replay for risk-calculation processing; details={}", Map.of(
                "eventType", "BATCH_INGESTED_EVENT_PUBLISHED",
                "eventId", batchIngestedEvent.getEventId(),
                "batchId", integrationEvent.getBatchId(),
                "bankId", integrationEvent.getBankId()
            ));
        } catch (Exception e) {
            log.error("❌ Error in BatchCompletedIntegrationAdapter: {}", e.getMessage(), e);
            throw e;
        }
    }
}
