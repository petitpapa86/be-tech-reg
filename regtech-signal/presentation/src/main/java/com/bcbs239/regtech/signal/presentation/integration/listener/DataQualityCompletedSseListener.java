package com.bcbs239.regtech.signal.presentation.integration.listener;

import com.bcbs239.regtech.core.domain.events.integration.DataQualityCompletedInboundEvent;
import com.bcbs239.regtech.signal.domain.SignalEvent;
import com.bcbs239.regtech.signal.infrastructure.SsePublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for DataQualityCompletedInboundEvent and publishes it to connected SSE clients.
 */
@Component("signalDataQualityCompletedEventListener")
public class DataQualityCompletedSseListener {
    private static final Logger log = LoggerFactory.getLogger(DataQualityCompletedSseListener.class);

    private final SsePublisher publisher;
    private final ObjectMapper objectMapper;

    public DataQualityCompletedSseListener(SsePublisher publisher, ObjectMapper objectMapper) {
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void on(DataQualityCompletedInboundEvent event) {
        if (event == null) return;

        try {
            if (!event.isValid()) {
                log.warn("Received invalid DataQualityCompletedInboundEvent, skipping SSE publish: batchId={}", event.getBatchId());
                return;
            }

            String payload = objectMapper.writeValueAsString(event);
            SignalEvent signal = new SignalEvent("data-quality.completed", payload);
            publisher.publish(signal);
            log.info("Published DataQualityCompletedInboundEvent to SSE: batchId={}, eventId={}", event.getBatchId(), signal.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DataQualityCompletedInboundEvent for SSE (batchId={})", event.getBatchId(), e);
        } catch (Exception e) {
            log.error("Unexpected error while publishing DataQualityCompletedInboundEvent to SSE (batchId={})", event.getBatchId(), e);
        }
    }
}
