package com.bcbs239.regtech.reportgeneration.application.integration;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.events.integration.ComplianceReportGeneratedIntegrationEvent;
import com.bcbs239.regtech.reportgeneration.application.integration.events.ReportGenerationFailedIntegrationEvent;
import com.bcbs239.regtech.reportgeneration.application.integration.events.ReportGeneratedIntegrationEvent;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGeneratedEvent;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGenerationFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Deprecated application-layer placeholder for the publisher.
 * The actual outbound messaging implementation has been moved to the
 * infrastructure layer: com.bcbs239.regtech.reportgeneration.infrastructure.messaging.outbound
 *
 * This class remains to avoid breaking references during the transition,
 * but it is not a Spring component and will no longer publish events.
 */
@Slf4j
@Deprecated
public class ReportGenerationEventPublisher {

    // Intentionally not a Spring bean. Use infrastructure publisher instead.
    private IIntegrationEventBus integrationEventBus = null;

    public ReportGenerationEventPublisher() {
        // no-op constructor: integration handled in infrastructure
        log.warn("ReportGenerationEventPublisher (application) is deprecated; inbound events are published by the infrastructure publisher.");
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReportGeneratedEvent(ReportGeneratedEvent event) {
        log.warn("Application-layer ReportGenerationEventPublisher invoked for report {} - ignored. Use infrastructure publisher.", event == null ? "<null>" : event.getReportId().value());
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleReportGenerationFailedEvent(ReportGenerationFailedEvent event) {
        log.warn("Application-layer ReportGenerationEventPublisher invoked for failed report {} - ignored. Use infrastructure publisher.", event == null ? "<null>" : event.getReportId().value());
    }

    // Keep logging helpers minimal to avoid unused-private warnings
    private void logEventPublished(ReportGeneratedEvent event) { /* no-op */ }
    private void logEventPublished(ReportGenerationFailedEvent event) { /* no-op */ }
    private void logEventSkipped(ReportGeneratedEvent event) { /* no-op */ }
    private void logEventSkipped(ReportGenerationFailedEvent event) { /* no-op */ }
    private void logEventPublishingError(ReportGeneratedEvent event, Exception ex) { /* no-op */ }
    private void logEventPublishingError(ReportGenerationFailedEvent event, Exception ex) { /* no-op */ }
}
