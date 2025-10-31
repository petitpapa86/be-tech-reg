package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class SagaManager {
    private final Function<AbstractSaga<?>, Result<SagaId>> sagaSaver;
    private final Function<SagaId, Maybe<AbstractSaga<?>>> sagaLoader;
    private final CommandDispatcher commandDispatcher;
    private final ApplicationEventPublisher eventPublisher;
    private final Supplier<Instant> currentTimeSupplier;
    private final SagaClosures.TimeoutScheduler timeoutScheduler;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> SagaId startSaga(Class<? extends AbstractSaga<T>> sagaClass, T data) {
        SagaId sagaId = SagaId.generate();
        AbstractSaga<T> saga = createSagaInstance(sagaClass, sagaId, data);

        // Handle the start event synchronously to initialize the saga
        SagaStartedEvent startEvent = new SagaStartedEvent(sagaId, saga.getSagaType(), currentTimeSupplier);
        saga.handle(startEvent);

        sagaSaver.apply(saga);

        LoggingConfiguration.createStructuredLog("SAGA_STARTED", Map.of(
            "sagaId", sagaId,
            "sagaType", sagaClass.getSimpleName()
        ));

        // Publish the start event and dispatch commands AFTER the transaction commits when available
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        // Dispatch commands (this will clear the saga's in-memory command list)
                        // Use dispatchNow to publish immediately because we're already in afterCommit
                        saga.getCommandsToDispatch().forEach(commandDispatcher::dispatchNow);
                        // Publish lifecycle event for saga started
                        eventPublisher.publishEvent(startEvent);
                    } catch (Exception e) {
                        LoggingConfiguration.createStructuredLog("SAGA_POST_COMMIT_ACTION_FAILED", Map.of(
                            "sagaId", sagaId,
                            "error", e.getMessage()
                        ));
                    }
                }
            });
        } else {
            // No transaction synchronization (e.g. in unit tests) - perform post-save actions immediately
            try {
                saga.getCommandsToDispatch().forEach(commandDispatcher::dispatch);
                eventPublisher.publishEvent(startEvent);
            } catch (Exception e) {
                LoggingConfiguration.createStructuredLog("SAGA_POST_COMMIT_ACTION_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", e.getMessage()
                ));
            }
        }

        return sagaId;
    }

    public void processEvent(SagaMessage event) {
        Maybe<AbstractSaga<?>> maybeSaga = sagaLoader.apply(event.getSagaId());
        if (maybeSaga.isEmpty()) {
            throw new SagaNotFoundException(event.getSagaId());
        }

        AbstractSaga<?> saga = maybeSaga.getValue();

        saga.handle(event);
        sagaSaver.apply(saga);

        dispatchCommands(saga);
        publishSagaLifecycleEvent(saga);
    }

    private void dispatchCommands(AbstractSaga<?> saga) {
        // Dispatch pending saga commands
        saga.getCommandsToDispatch().forEach(commandDispatcher::dispatch);
    }

    private void publishSagaLifecycleEvent(AbstractSaga<?> saga) {
        if (saga.getStatus() == SagaStatus.COMPLETED) {
            eventPublisher.publishEvent(new SagaCompletedEvent(saga.getId(), saga.getSagaType(), currentTimeSupplier));
        } else if (saga.getStatus() == SagaStatus.FAILED) {
            eventPublisher.publishEvent(new SagaFailedEvent(saga.getId(), saga.getSagaType(), currentTimeSupplier));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> AbstractSaga<T> createSagaInstance(Class<? extends AbstractSaga<T>> sagaClass, SagaId sagaId, T data) {
        try {
            Constructor<?> constructor = sagaClass.getDeclaredConstructor(SagaId.class, data.getClass(), SagaClosures.TimeoutScheduler.class);
            return (AbstractSaga<T>) constructor.newInstance(sagaId, data, timeoutScheduler);
        } catch (Exception e) {
            throw new SagaCreationException("Failed to create saga instance: " + sagaClass.getSimpleName(), e);
        }
    }
}