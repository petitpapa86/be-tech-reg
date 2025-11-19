package com.bcbs239.regtech.riskcalculation.application.integration;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationCompletedEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration adapter for handling batch calculation completion events.
 * Similar to data-quality module pattern, this adapter listens to domain events
 * and coordinates with external systems and downstream modules.
 * 
 * Responsibilities:
 * - Listen to BatchCalculationCompletedEvent and BatchCalculationFailedEvent
 * - Publish integration events to notify downstream modules (billing, reporting)
 * - Handle cross-module communication and event transformation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchCompletedIntegrationAdapter {
    
    private final RiskCalculationEventPublisher eventPublisher;

    /**
     * Handles successful batch calculation completion.
     * Publishes integration events to notify downstream modules.
     * 
     * @param event The batch calculation completed domain event
     */
    @EventListener
    @Transactional
    public void handleBatchCalculationCompleted(BatchCalculationCompletedEvent event) {
        log.info("Handling BatchCalculationCompletedEvent for batch: {}", event.getBatchId().value());
        
        try {
            // Publish integration event for downstream modules
            Result<Void> publishResult = eventPublisher.publishBatchCalculationCompleted(event);
            
            if (publishResult.isFailure()) {
                log.error("Failed to publish BatchCalculationCompletedEvent for batch: {}", 
                    event.getBatchId().value());
                // Note: We don't throw here to avoid rolling back the calculation
                // The event will be retried by the event retry mechanism
            } else {
                log.info("Successfully published BatchCalculationCompletedEvent for batch: {}", 
                    event.getBatchId().value());
            }
            
        } catch (Exception e) {
            log.error("Unexpected error handling BatchCalculationCompletedEvent for batch: {}", 
                event.getBatchId().value(), e);
            // Note: We don't re-throw to avoid rolling back the successful calculation
        }
    }
    
    /**
     * Handles failed batch calculation.
     * Publishes failure events to notify downstream modules and support teams.
     * 
     * @param event The batch calculation failed domain event
     */
    @EventListener
    @Transactional
    public void handleBatchCalculationFailed(BatchCalculationFailedEvent event) {
        log.error("Handling BatchCalculationFailedEvent for batch: {} with error: {}", 
            event.getBatchId().value(), event.getErrorMessage());
        
        try {
            // Publish integration event for downstream modules
            Result<Void> publishResult = eventPublisher.publishBatchCalculationFailed(event);
            
            if (publishResult.isFailure()) {
                log.error("Failed to publish BatchCalculationFailedEvent for batch: {}", 
                    event.getBatchId().value());
            } else {
                log.info("Successfully published BatchCalculationFailedEvent for batch: {}", 
                    event.getBatchId().value());
            }
            
            // Log structured error for monitoring and alerting
            log.info(
                "Risk calculation failed for batch: {} , details={}",
                event.getBatchId().value(),
                java.util.Map.of(
                    "batchId", event.getBatchId().value(),
                    "bankId", event.getBankId().value(),
                    "errorMessage", event.getErrorMessage(),
                    "eventType", "BatchCalculationFailed",
                    "module", "risk-calculation"
                )
            );
            
        } catch (Exception e) {
            log.error("Unexpected error handling BatchCalculationFailedEvent for batch: {}", 
                event.getBatchId().value(), e);
        }
    }
}