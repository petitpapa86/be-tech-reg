package com.bcbs239.regtech.core.application;

import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.saga.SagaCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class CommandDispatcher {
     private final ApplicationEventPublisher eventPublisher;
     private final ILogger logger;

    public void dispatch(SagaCommand command) {
        // Diagnostic structured log to trace commands being dispatched
        try {
            logger.createStructuredLog("SAGA_COMMAND_PUBLISHED", java.util.Map.of(
                "sagaId", command.sagaId(),
                "commandType", command.commandType()
            ));
        } catch (Exception e) {
            // ignore logging failure
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        logger.createStructuredLog("SAGA_COMMAND_AFTER_COMMIT", java.util.Map.of(
                            "sagaId", command.sagaId(),
                            "commandType", command.commandType()
                        ));
                        eventPublisher.publishEvent(command);
                    } catch (Exception e) {
                        logger.createStructuredLog("SAGA_COMMAND_PUBLISH_FAILED", java.util.Map.of(
                            "sagaId", command.sagaId(),
                            "commandType", command.commandType(),
                            "error", e.getMessage()
                        ));
                    }
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
            logger.createStructuredLog("SAGA_COMMAND_PUBLISHED_IMMEDIATE", java.util.Map.of(
                "sagaId", command.sagaId(),
                "commandType", command.commandType()
            ));
        } catch (Exception e) {
            // ignore logging failure
        }
        eventPublisher.publishEvent(command);
    }
}

