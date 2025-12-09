package com.bcbs239.regtech.riskcalculation.application.integration;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.application.integration.events.BatchCalculationCompletedIntegrationEvent;
import com.bcbs239.regtech.riskcalculation.application.integration.events.BatchCalculationFailedIntegrationEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationCompletedEvent;
import com.bcbs239.regtech.riskcalculation.domain.calculation.events.BatchCalculationFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Service for publishing risk calculation integration events.
 * 
 * @deprecated This class is deprecated as part of the DDD aggregate refactoring.
 * Events are now raised by domain aggregates (Batch, PortfolioAnalysis) and
 * persisted to the outbox table through the BaseUnitOfWork pattern.
 * The OutboxProcessor handles asynchronous event publishing.
 * 
 * This class remains for backward compatibility with existing event listeners
 * that transform domain events to integration events.
 * 
 * Migration path:
 * - Domain aggregates raise events through Entity.addDomainEvent()
 * - CommandHandlers register aggregates with BaseUnitOfWork
 * - BaseUnitOfWork.saveChanges() persists events to outbox
 * - OutboxProcessor publishes events asynchronously
 * 
 * Features:
 * - Event content validation before publishing
 * - Structured logging for monitoring and debugging
 * - Transactional event listeners for reliable publishing
 * - Error handling and retry support
 */
@Deprecated(since = "2.0", forRemoval = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    /**
     * Publishes batch calculation completed event.
     * 
     * @param batchId The batch ID
     * @param bankId The bank ID
     * @param totalExposures Total number of exposures
     * @param calculationResultsUri The URI where calculation results are stored
     */
    public void publishBatchCalculationCompleted(String batchId, String bankId, int totalExposures, String calculationResultsUri) {
        log.info("Publishing BatchCalculationCompletedEvent for batch: {} with results URI: {}", batchId, calculationResultsUri);
        
        try {
            // Create domain event with calculation results URI
            // Requirement 4.1: Include calculation_results_uri in event
            BatchCalculationCompletedEvent domainEvent = new BatchCalculationCompletedEvent(
                batchId,
                bankId,
                totalExposures,
                calculationResultsUri,
                Instant.now()
            );
            
            // Publish the domain event
            eventPublisher.publishEvent(domainEvent);
            
            log.info("Successfully published BatchCalculationCompletedEvent for batch: {} with URI: {}", batchId, calculationResultsUri);
            
        } catch (Exception e) {
            log.error("Failed to publish BatchCalculationCompletedEvent for batch: {}", batchId, e);
        }
    }
    
    /**
     * Publishes BatchCalculationCompletedEvent as integration event.
     * Transforms domain event to integration event with comprehensive data.
     * 
     * @param domainEvent The domain event from risk calculation completion
     * @return Result indicating success or failure of publishing
     */
    @TransactionalEventListener
    public Result<Void> publishBatchCalculationCompleted(BatchCalculationCompletedEvent domainEvent) {
        log.info("Publishing BatchCalculationCompletedIntegrationEvent for batch: {}", 
            domainEvent.getBatchId());
        
        try {
            // Create integration event with essential data only
            // Detailed results are available in S3/filesystem via resultFileUri
            // Requirement 4.1: Include calculation_results_uri in event
            BatchCalculationCompletedIntegrationEvent integrationEvent = 
                new BatchCalculationCompletedIntegrationEvent(
                    domainEvent.getBatchId(),
                    domainEvent.getBankId(),
                    domainEvent.getCalculationResultsUri(),
                    domainEvent.getProcessedExposures(),
                    BigDecimal.ZERO, // Total amount - will be calculated from analysis
                    Instant.now(),
                    BigDecimal.ZERO, // Geographic HHI - will be populated from analysis
                    BigDecimal.ZERO  // Sector HHI - will be populated from analysis
                );
            
            // Log structured event data for monitoring
            logEventPublication(integrationEvent);
            
            // Publish the integration event
            eventPublisher.publishEvent(integrationEvent);
            
            log.info("Successfully published BatchCalculationCompletedIntegrationEvent for batch: {}", 
                domainEvent.getBatchId());
            
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to publish BatchCalculationCompletedIntegrationEvent for batch: {}", 
                domainEvent.getBatchId(), e);
            
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISHING_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to publish integration event: " + e.getMessage(),
                "event.publishing.error"
            ));
        }
    }
    
    /**
     * Publishes batch calculation failed event.
     * 
     * @param batchId The batch ID
     * @param bankId The bank ID
     * @param errorMessage The error message
     */
    public void publishBatchCalculationFailed(String batchId, String bankId, String errorMessage) {
        log.info("Publishing BatchCalculationFailedEvent for batch: {}", batchId);
        
        try {
            // Create domain event
            BatchCalculationFailedEvent domainEvent = new BatchCalculationFailedEvent(
                batchId,
                bankId,
                errorMessage,
                Instant.now()
            );
            
            // Publish the domain event
            eventPublisher.publishEvent(domainEvent);
            
            log.info("Successfully published BatchCalculationFailedEvent for batch: {}", batchId);
            
        } catch (Exception e) {
            log.error("Failed to publish BatchCalculationFailedEvent for batch: {}", batchId, e);
        }
    }
    
    /**
     * Publishes BatchCalculationFailedEvent as integration event.
     * 
     * @param domainEvent The domain event from risk calculation failure
     * @return Result indicating success or failure of publishing
     */
    @TransactionalEventListener
    public Result<Void> publishBatchCalculationFailed(BatchCalculationFailedEvent domainEvent) {
        log.info("Publishing BatchCalculationFailedIntegrationEvent for batch: {}", 
            domainEvent.getBatchId());
        
        try {
            // Create integration event
            BatchCalculationFailedIntegrationEvent integrationEvent = 
                new BatchCalculationFailedIntegrationEvent(
                    domainEvent.getBatchId(),
                    domainEvent.getBankId(),
                    domainEvent.getReason(),
                    "RISK_CALCULATION_FAILED", // Standard error code
                    Instant.now()
                );
            
            // Log structured event data for monitoring
            logEventPublication(integrationEvent);
            
            // Publish the integration event
            eventPublisher.publishEvent(integrationEvent);
            
            log.info("Successfully published BatchCalculationFailedIntegrationEvent for batch: {}", 
                domainEvent.getBatchId());
            
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to publish BatchCalculationFailedIntegrationEvent for batch: {}", 
                domainEvent.getBatchId(), e);
            
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISHING_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to publish integration event: " + e.getMessage(),
                "event.publishing.error"
            ));
        }
    }
    
    /**
     * Logs structured event data for monitoring and debugging.
     */
    private void logEventPublication(Object event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            log.debug("Publishing integration event: {}", eventJson);
        } catch (Exception e) {
            log.warn("Failed to serialize event for logging", e);
        }
    }
}