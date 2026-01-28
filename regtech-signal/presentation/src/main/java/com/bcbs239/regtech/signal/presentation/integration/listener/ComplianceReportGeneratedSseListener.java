package com.bcbs239.regtech.signal.presentation.integration.listener;

import com.bcbs239.regtech.core.domain.events.integration.ComplianceReportGeneratedInboundEvent;
import com.bcbs239.regtech.signal.domain.SignalEvent;
import com.bcbs239.regtech.signal.infrastructure.SsePublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Publishes `ComplianceReportGeneratedInboundEvent` occurrences to SSE clients.
 */
@Component("signalComplianceReportGeneratedEventListener")
public class ComplianceReportGeneratedSseListener {
    private static final Logger log = LoggerFactory.getLogger(ComplianceReportGeneratedSseListener.class);

    private final SsePublisher publisher;
    private final ObjectMapper objectMapper;

    public ComplianceReportGeneratedSseListener(SsePublisher publisher, ObjectMapper objectMapper) {
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void on(ComplianceReportGeneratedInboundEvent event) {
        if (event == null) return;

        try {
            String payload = objectMapper.writeValueAsString(event);
            SignalEvent signal = new SignalEvent("report.completed", payload);
            publisher.publish(signal);
            log.info("Published ComplianceReportGeneratedInboundEvent to SSE: reportId={}, eventId={}", event.getReportId(), signal.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ComplianceReportGeneratedInboundEvent for SSE (reportId={})", event.getReportId(), e);
            throw new RuntimeException("Failed to serialize SSE payload", e);
        } catch (Exception e) {
            log.error("Unexpected error while publishing ComplianceReportGeneratedInboundEvent to SSE (reportId={})", event.getReportId(), e);
            throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
        }
    }
}
