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

import java.time.Instant;

/**
 * Service for publishing risk calculation integration events.
 * Handles event content validation, structured logging, and reliable publishing
 * using transactional event listeners.
 * 
 * Features:
 * - Event content validation before publishing
 * - Structured logging for monitoring and debugging
 * - Transactional event listeners for reliable publishing
 * - Error handling and retry support
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
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
            domainEvent.getBatchId().value());
        
        try {
            // Validate event content
            Result<Void> validationResult = validateCompletedEvent(domainEvent);
            if (validationResult.isFailure()) {
                log.error("Event validation failed for batch: {}", domainEvent.getBatchId().value());
                return validationResult;
            }
            
            // Create integration event with essential data only
            // Detailed results are available in S3/filesystem via resultFileUri
            BatchCalculationCompletedIntegrationEvent integrationEvent = 
                new BatchCalculationCompletedIntegrationEvent(
                    domainEvent.getBatchId().value(),
                    domainEvent.getBankId().value(),
                    domainEvent.getResultFileUri().uri(),
                    domainEvent.getTotalExposures().count(),
                    domainEvent.getTotalAmountEur().value(),
                    Instant.now(),
                    domainEvent.getConcentrationIndices().geographicHerfindahl().value(),
                    domainEvent.getConcentrationIndices().sectorHerfindahl().value()
                );
            
            // Log structured event data for monitoring
            logEventPublication(integrationEvent);
            
            // Publish the integration event
            eventPublisher.publishEvent(integrationEvent);
            
            log.info("Successfully published BatchCalculationCompletedIntegrationEvent for batch: {}", 
                domainEvent.getBatchId().value());
            
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to publish BatchCalculationCompletedIntegrationEvent for batch: {}", 
                domainEvent.getBatchId().value(), e);
            
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISHING_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to publish integration event: " + e.getMessage(),
                "event.publishing.error"
            ));
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
            domainEvent.getBatchId().value());
        
        try {
            // Validate event content
            Result<Void> validationResult = validateFailedEvent(domainEvent);
            if (validationResult.isFailure()) {
                log.error("Event validation failed for batch: {}", domainEvent.getBatchId().value());
                return validationResult;
            }
            
            // Create integration event
            BatchCalculationFailedIntegrationEvent integrationEvent = 
                new BatchCalculationFailedIntegrationEvent(
                    domainEvent.getBatchId().value(),
                    domainEvent.getBankId().value(),
                    domainEvent.getErrorMessage(),
                    "RISK_CALCULATION_FAILED", // Standard error code
                    Instant.now()
                );
            
            // Log structured event data for monitoring
            logEventPublication(integrationEvent);
            
            // Publish the integration event
            eventPublisher.publishEvent(integrationEvent);
            
            log.info("Successfully published BatchCalculationFailedIntegrationEvent for batch: {}", 
                domainEvent.getBatchId().value());
            
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("Failed to publish BatchCalculationFailedIntegrationEvent for batch: {}", 
                domainEvent.getBatchId().value(), e);
            
            return Result.failure(ErrorDetail.of(
                "EVENT_PUBLISHING_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to publish integration event: " + e.getMessage(),
                "event.publishing.error"
            ));
        }
    }
    
    /**
     * Validates BatchCalculationCompletedEvent content.
     */
    private Result<Void> validateCompletedEvent(BatchCalculationCompletedEvent event) {
        if (event.getBatchId() == null || event.getBatchId().value().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_EVENT_CONTENT",
                ErrorType.BUSINESS_RULE_ERROR,
                "Batch ID is required for event publishing",
                "event.validation.batch.id.required"
            ));
        }
        
        if (event.getBankId() == null || event.getBankId().value().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_EVENT_CONTENT",
                ErrorType.BUSINESS_RULE_ERROR,
                "Bank ID is required for event publishing",
                "event.validation.bank.id.required"
            ));
        }
        
        if (event.getResultFileUri() == null || event.getResultFileUri().uri().trim().isEmpty()) {
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
     * Validates BatchCalculationFailedEvent content.
     */
    private Result<Void> validateFailedEvent(BatchCalculationFailedEvent event) {
        if (event.getBatchId() == null || event.getBatchId().value().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_EVENT_CONTENT",
                ErrorType.BUSINESS_RULE_ERROR,
                "Batch ID is required for event publishing",
                "event.validation.batch.id.required"
            ));
        }
        
        if (event.getErrorMessage() == null || event.getErrorMessage().trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_EVENT_CONTENT",
                ErrorType.BUSINESS_RULE_ERROR,
                "Error message is required for failed event publishing",
                "event.validation.error.message.required"
            ));
        }
        
        return Result.success(null);
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