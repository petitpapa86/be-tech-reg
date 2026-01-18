package com.bcbs239.regtech.signal.presentation.integration.listener;

import com.bcbs239.regtech.core.domain.context.CorrelationContext;
import com.bcbs239.regtech.core.domain.events.integration.BatchCompletedInboundEvent;
import com.bcbs239.regtech.signal.application.SignalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("signalBatchCompletedIntegrationEventListener")
@Slf4j
public class BatchCompletedIntegrationEventListener {
    private final SignalService signalService;
    private final ObjectMapper objectMapper;

    public BatchCompletedIntegrationEventListener(SignalService signalService, ObjectMapper objectMapper) {
        this.signalService = signalService;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void on(BatchCompletedInboundEvent event) {
        log.info("Signal received BatchCompletedInboundEvent: batchId={}, bankId={}, totalExposures={}, isInboxReplay={}, isOutboxReplay={}"
                , event.getBatchId(), event.getBankId(), event.getTotalExposures(), CorrelationContext.isInboxReplay(), CorrelationContext.isOutboxReplay());

        if (CorrelationContext.isInboxReplay()) {
            return; // skip replayed messages
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("batchId", event.getBatchId());
        payload.put("bankId", event.getBankId());
        payload.put("s3Uri", event.getS3Uri());
        payload.put("totalExposures", event.getTotalExposures());
        payload.put("fileSizeBytes", event.getFileSizeBytes());
        payload.put("completedAt", event.getCompletedAt());
        payload.put("status", "COMPLETED");

        try {
            String json = objectMapper.writeValueAsString(payload);

            // publish as SSE event type 'batch.completed'
            signalService.publish("batch.completed", json);
            log.info("Published SSE batch.completed for batchId={}", event.getBatchId());
        } catch (Exception e) {
            log.error("Failed to publish SSE for batch {}", event.getBatchId(), e);
        }
    }
}
