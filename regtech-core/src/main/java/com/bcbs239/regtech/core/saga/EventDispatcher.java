package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
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
    public void handleEvent(Object event) {
        try {
            // Support both direct payload publishing and PayloadApplicationEvent wrapping
            SagaMessage sagaEvent = null;
            if (event instanceof SagaMessage sm) {
                sagaEvent = sm;
            } else if (event instanceof ApplicationEvent ae) {
                // If Spring wrapped the payload (e.g. PayloadApplicationEvent), extract payload
                if (ae instanceof PayloadApplicationEvent<?> pae) {
                    Object payload = pae.getPayload();
                    if (payload instanceof SagaMessage sm2) {
                        sagaEvent = sm2;
                    }
                } else {
                    // Also support plain ApplicationEvent where the source is the SagaMessage
                    Object source = ae.getSource();
                    if (source instanceof SagaMessage sm3) {
                        sagaEvent = sm3;
                    }
                }
            }

            if (sagaEvent != null) {
                dispatchToSaga(sagaEvent);
            }
        } catch (Exception e) {
            LoggingConfiguration.createStructuredLog("EVENT_DISPATCHER_ERROR", Map.of(
                "error", e.getMessage()
            ));
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