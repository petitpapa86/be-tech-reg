package com.bcbs239.regtech.reportgeneration.application.integration;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.ComplianceReportGeneratedIntegrationEvent;
import com.bcbs239.regtech.reportgeneration.application.integration.events.ReportGenerationFailedIntegrationEvent;
import com.bcbs239.regtech.reportgeneration.application.integration.events.ReportGeneratedIntegrationEvent;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGeneratedEvent;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGenerationFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Publisher that listens to report generation domain events and publishes
 * integration events to notify other bounded contexts.
 * 
 * This follows the outbox pattern - the domain event triggers the integration event
 * publication within the same transaction boundary. The OutboxProcessor handles
 * automatic retry of failed event publications.
 * 
 * Requirements: 14.4, 14.5, 15.5
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationEventPublisher {

    private final IIntegrationEventBus integrationEventBus;

    /**
     * Handles ReportGeneratedEvent domain events and publishes integration events.
     * Uses TransactionalEventListener to ensure the integration event is published
     * within the same transaction as the domain event.
     * 
     * The OutboxProcessor will automatically retry failed publications with exponential
     * backoff (every 60 seconds, up to 5 retries).
     * 
     * Requirements: 14.4, 14.5, 15.1, 15.2, 15.5
     * 
     * @param event The domain event from the GeneratedReport aggregate
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReportGeneratedEvent(ReportGeneratedEvent event) {
        // Skip if this is an outbox replay to avoid duplicate publishing
        if (CorrelationContext.isOutboxReplay()) {
            logEventSkipped(event);
            return;
        }

        try {
            // Create integration event from domain event
            ComplianceReportGeneratedIntegrationEvent integrationEvent = new ComplianceReportGeneratedIntegrationEvent(
                    event.getReportId().value().toString(),
                    event.getBatchId().value(),
                    event.getBankId().value(),
                    event.getReportType().name(),
                    event.getReportingDate().value().toString(),
                    event.getHtmlS3Uri() != null ? event.getHtmlS3Uri().value() : null,
                    event.getXbrlS3Uri() != null ? event.getXbrlS3Uri().value() : null,
                    event.getHtmlPresignedUrl() != null ? event.getHtmlPresignedUrl().url() : null,
                    event.getXbrlPresignedUrl() != null ? event.getXbrlPresignedUrl().url() : null,
                    event.getHtmlFileSize() != null ? event.getHtmlFileSize().bytes() : null,
                    event.getXbrlFileSize() != null ? event.getXbrlFileSize().bytes() : null,
                    event.getOverallQualityScore(),
                    event.getComplianceStatus().name(),
                    event.getGenerationDuration() != null ? event.getGenerationDuration().toMillis() : null,
                    event.getGeneratedAt(),
                    CorrelationContext.correlationId()
            );

            // Publish to other bounded contexts
            // The OutboxProcessor will handle retry if publication fails
            integrationEventBus.publish(integrationEvent);

            // Publish module-specific ReportGeneratedIntegrationEvent as well
            try {
                ReportGeneratedIntegrationEvent reportEvent = new ReportGeneratedIntegrationEvent(
                        event.getReportId().value().toString(),
                        event.getBatchId().value(),
                        event.getBankId().value(),
                        event.getReportType().name(),
                        event.getReportingDate().value().toString(),
                        event.getHtmlS3Uri() != null ? event.getHtmlS3Uri().value() : null,
                        event.getXbrlS3Uri() != null ? event.getXbrlS3Uri().value() : null,
                        event.getHtmlPresignedUrl() != null ? event.getHtmlPresignedUrl().url() : null,
                        event.getXbrlPresignedUrl() != null ? event.getXbrlPresignedUrl().url() : null,
                        event.getHtmlFileSize() != null ? event.getHtmlFileSize().bytes() : null,
                        event.getXbrlFileSize() != null ? event.getXbrlFileSize().bytes() : null,
                        event.getOverallQualityScore(),
                        event.getComplianceStatus().name(),
                        event.getGenerationDuration() != null ? event.getGenerationDuration().toMillis() : null,
                        event.getGeneratedAt(),
                        CorrelationContext.correlationId()
                );

                integrationEventBus.publish(reportEvent);
                log.info("Published ReportGeneratedIntegrationEvent for report: {}", event.getReportId().value());
            } catch (Exception ex) {
                log.error("Failed to publish ReportGeneratedIntegrationEvent for report: {}", event.getReportId().value(), ex);
                throw ex;
            }

            logEventPublished(event);

        } catch (Exception ex) {
            logEventPublishingError(event, ex);
            // Re-throw to ensure transaction rollback
            // The OutboxProcessor will retry the event publication
            throw ex;
        }
    }

    /**
     * Handles ReportGenerationFailedEvent domain events and publishes integration events.
     * 
     * Requirements: 14.4, 14.5, 15.5
     * 
     * @param event The domain event from the GeneratedReport aggregate
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReportGenerationFailedEvent(ReportGenerationFailedEvent event) {
        // Skip if this is an outbox replay to avoid duplicate publishing
        if (CorrelationContext.isOutboxReplay()) {
            logEventSkipped(event);
            return;
        }

        try {
            // Create integration event from domain event
            ReportGenerationFailedIntegrationEvent integrationEvent = new ReportGenerationFailedIntegrationEvent(
                    event.getReportId().value().toString(),
                    event.getBatchId().value(),
                    event.getBankId().value(),
                    event.getFailureReason().message(),
                    "REPORT_GENERATION_FAILED", // Standard error code
                    event.getFailedAt(),
                    CorrelationContext.correlationId()
            );

            // Publish to other bounded contexts
            // The OutboxProcessor will handle retry if publication fails
            integrationEventBus.publish(integrationEvent);

            logEventPublished(event);

        } catch (Exception ex) {
            logEventPublishingError(event, ex);
            // Re-throw to ensure transaction rollback
            // The OutboxProcessor will retry the event publication
            throw ex;
        }
    }

    private void logEventPublished(ReportGeneratedEvent event) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "ReportGeneratedEvent");
        details.put("reportId", event.getReportId().value());
        details.put("batchId", event.getBatchId().value());
        details.put("bankId", event.getBankId().value());
        details.put("reportType", event.getReportType().name());
        details.put("overallQualityScore", event.getOverallQualityScore());
        details.put("complianceStatus", event.getComplianceStatus().name());
        details.put("generatedAt", event.getGeneratedAt().toString());

        log.info("Published ReportGeneratedIntegrationEvent for report: {} details={}", 
            event.getReportId().value(), details);
    }

    private void logEventPublished(ReportGenerationFailedEvent event) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "ReportGenerationFailedEvent");
        details.put("reportId", event.getReportId().value());
        details.put("batchId", event.getBatchId().value());
        details.put("bankId", event.getBankId().value());
        details.put("failureReason", event.getFailureReason().message());
        details.put("failedAt", event.getFailedAt().toString());

        log.info("Published ReportGenerationFailedIntegrationEvent for report: {} details={}", 
            event.getReportId().value(), details);
    }

    private void logEventSkipped(ReportGeneratedEvent event) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "ReportGeneratedEvent");
        details.put("reportId", event.getReportId().value());
        details.put("batchId", event.getBatchId().value());
        details.put("reason", "Outbox replay - skipping to avoid duplicate");

        log.info("Skipped ReportGeneratedIntegrationEvent publishing for report: {} details={}", 
            event.getReportId().value(), details);
    }

    private void logEventSkipped(ReportGenerationFailedEvent event) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "ReportGenerationFailedEvent");
        details.put("reportId", event.getReportId().value());
        details.put("batchId", event.getBatchId().value());
        details.put("reason", "Outbox replay - skipping to avoid duplicate");

        log.info("Skipped ReportGenerationFailedIntegrationEvent publishing for report: {} details={}", 
            event.getReportId().value(), details);
    }

    private void logEventPublishingError(ReportGeneratedEvent event, Exception ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "ReportGeneratedEvent");
        details.put("reportId", event.getReportId().value());
        details.put("batchId", event.getBatchId().value());
        details.put("bankId", event.getBankId().value());
        details.put("errorMessage", ex.getMessage());

        log.error("Failed to publish ReportGeneratedIntegrationEvent for report: {} details={}", 
            event.getReportId().value(), details, ex);
    }

    private void logEventPublishingError(ReportGenerationFailedEvent event, Exception ex) {
        Map<String, Object> details = new HashMap<>();
        details.put("eventType", "ReportGenerationFailedEvent");
        details.put("reportId", event.getReportId().value());
        details.put("batchId", event.getBatchId().value());
        details.put("bankId", event.getBankId().value());
        details.put("errorMessage", ex.getMessage());

        log.error("Failed to publish ReportGenerationFailedIntegrationEvent for report: {} details={}", 
            event.getReportId().value(), details, ex);
    }
}
