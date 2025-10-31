package com.bcbs239.regtech.core.saga;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import com.bcbs239.regtech.core.config.LoggingConfiguration;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(command);
                }
            });
        } else {
            eventPublisher.publishEvent(command);
        }
    }

    /**
     * Publish the given command immediately (synchronously) without attempting to register
     * additional transaction synchronizations. This is useful when called from within
     * an existing afterCommit callback where registering a new synchronization would be too late.
     */
    public void dispatchNow(SagaCommand command) {
        try {
            LoggingConfiguration.createStructuredLog("SAGA_COMMAND_PUBLISHED_IMMEDIATE", java.util.Map.of(
                "sagaId", command.getSagaId(),
                "commandType", command.commandType()
            ));
        } catch (Exception e) {
            // ignore logging failure
        }
        eventPublisher.publishEvent(command);
    }
}
