package com.bcbs239.regtech.riskcalculation.application.integration;

import com.bcbs239.regtech.core.domain.events.DomainEventBus;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedIntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.integrationevents.BatchIngestedEvent;
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

    private final DomainEventBus domainEventBus;

    /**
     * Handles successful batch calculation completion.
     * Publishes integration events to notify downstream modules.
     * 
     * @param integrationEvent The batch calculation completed domain event
     */
    @EventListener
    //@Transactional
    public void handleBatchCalculationCompleted(BatchCompletedIntegrationEvent integrationEvent) {
        log.info("Handling BatchCalculationCompletedEvent for batch: {}", integrationEvent.getBatchId());

        Result<Void> validationResult = validateCompletedEvent(integrationEvent);
        if (validationResult.isFailure()) {
            log.error("Event validation failed for batch: {}", integrationEvent.getBatchId());
        }

        BatchIngestedEvent batchIngestedEvent = new BatchIngestedEvent(
                integrationEvent.getBatchId(),
                integrationEvent.getBankId(),
                integrationEvent.getS3Uri(),
                integrationEvent.getTotalExposures(),
                integrationEvent.getFileSizeBytes(),
                integrationEvent.getCompletedAt()
        );

        // Publish as replay so existing data-quality handlers receive it
        // This will trigger the BatchIngestedEventListener to process the batch
        domainEventBus.publishAsReplay(batchIngestedEvent);
    }

    /**
     * Validates BatchCalculationCompletedEvent content.
     */
    private Result<Void> validateCompletedEvent(BatchCompletedIntegrationEvent event) {
        if (event.getBatchId() == null || event.getBatchId().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                    "INVALID_EVENT_CONTENT",
                    ErrorType.BUSINESS_RULE_ERROR,
                    "Batch ID is required for event publishing",
                    "event.validation.batch.id.required"
            ));
        }

        if (event.getBankId() == null || event.getBankId().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                    "INVALID_EVENT_CONTENT",
                    ErrorType.BUSINESS_RULE_ERROR,
                    "Bank ID is required for event publishing",
                    "event.validation.bank.id.required"
            ));
        }

        if (event.getS3Uri() == null || event.getS3Uri().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                    "INVALID_EVENT_CONTENT",
                    ErrorType.BUSINESS_RULE_ERROR,
                    "Result file URI is required for event publishing",
                    "event.validation.result.uri.required"
            ));
        }

        return Result.success(null);
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
            Result<Void> publishResult = null; // eventPublisher.publishBatchCalculationFailed(event);
            
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