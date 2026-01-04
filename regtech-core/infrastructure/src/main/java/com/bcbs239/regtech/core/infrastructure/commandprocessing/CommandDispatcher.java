package com.bcbs239.regtech.core.infrastructure.commandprocessing;

import com.bcbs239.regtech.core.domain.saga.SagaCommand;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component("infrastructureCommandDispatcher")
@RequiredArgsConstructor
public class CommandDispatcher {
    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);
    private final ApplicationEventPublisher eventPublisher;

    public void dispatch(SagaCommand command) {
        log.debug("Saga command published: sagaId={} commandType={}", 
            command.sagaId().id(), command.commandType());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        log.debug("Saga command after commit: sagaId={} commandType={}",
                            command.sagaId().id(), command.commandType());
                        eventPublisher.publishEvent(command);
                    } catch (Exception e) {
                        log.error("Saga command publish failed: sagaId={} commandType={}",
                            command.sagaId().id(), command.commandType(), e);
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
        log.debug("Saga command published immediately: sagaId={} commandType={}",
            command.sagaId().id(), command.commandType());
        eventPublisher.publishEvent(command);
    }
}

