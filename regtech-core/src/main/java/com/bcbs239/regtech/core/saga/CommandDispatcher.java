package com.bcbs239.regtech.core.saga;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import com.bcbs239.regtech.core.config.LoggingConfiguration;

@Component
@RequiredArgsConstructor
public class CommandDispatcher {
     private final ApplicationEventPublisher eventPublisher;

    public void dispatch(SagaCommand command) {
        // Diagnostic structured log to trace commands being dispatched
        try {
            LoggingConfiguration.createStructuredLog("SAGA_COMMAND_PUBLISHED", java.util.Map.of(
                "sagaId", command.getSagaId(),
                "commandType", command.commandType()
            ));
        } catch (Exception e) {
            // ignore logging failure
        }
        eventPublisher.publishEvent(command);
    }
}
