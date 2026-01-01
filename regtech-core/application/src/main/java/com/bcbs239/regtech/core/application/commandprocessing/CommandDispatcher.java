package com.bcbs239.regtech.core.application.commandprocessing;

import com.bcbs239.regtech.core.domain.saga.SagaCommand;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CommandDispatcher {
    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private final ApplicationEventPublisher eventPublisher;

    public void dispatch(SagaCommand command) {
        try {
            log.info("SAGA_COMMAND_PUBLISHED; details={}", Map.of(
                "sagaId", command.sagaId().id(),
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
                        log.info("SAGA_COMMAND_AFTER_COMMIT; details={}", Map.of(
                            "sagaId", command.sagaId().id(),
                            "commandType", command.commandType()
                        ));
                        eventPublisher.publishEvent(command);
                    } catch (Exception e) {
                        log.error("SAGA_COMMAND_PUBLISH_FAILED; details={}", Map.of(
                            "sagaId", command.sagaId().id(),
                            "commandType", command.commandType(),
                            "error", e.getMessage()
                        ), e);
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
            log.info("SAGA_COMMAND_PUBLISHED_IMMEDIATE; details={}", Map.of(
                "sagaId", command.sagaId().id(),
                "commandType", command.commandType()
            ));
        } catch (Exception e) {
            // ignore logging failure
        }
        eventPublisher.publishEvent(command);
    }
}
