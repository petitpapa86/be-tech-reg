package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventDispatcher {
    private final SagaManager sagaManager;

    @EventListener
    @Async("sagaTaskExecutor")
    public void handleEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent.getSource() instanceof SagaMessage sagaEvent) {
            dispatchToSaga(sagaEvent);
        }
    }

    private void dispatchToSaga(SagaMessage event) {
        try {
            sagaManager.processEvent(event);
        } catch (SagaNotFoundException e) {
            LoggingConfiguration.createStructuredLog("SAGA_NOT_FOUND", Map.of(
                "sagaId", event.getSagaId(),
                "eventType", event.eventType()
            ));
        } catch (Exception e) {
            LoggingConfiguration.createStructuredLog("EVENT_PROCESS_FAILED", Map.of(
                "sagaId", event.getSagaId(),
                "eventType", event.eventType(),
                "error", e.getMessage()
            ));
        }
    }
}