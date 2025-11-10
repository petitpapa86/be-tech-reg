package com.bcbs239.regtech.core.application.saga;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.logging.ILogger;
import com.bcbs239.regtech.core.domain.saga.*;
import com.bcbs239.regtech.core.infrastructure.commandprocessing.CommandDispatcher;
import com.bcbs239.regtech.core.infrastructure.saga.SagaCompletedEvent;
import com.bcbs239.regtech.core.infrastructure.saga.SagaCreationException;
import com.bcbs239.regtech.core.infrastructure.saga.SagaNotFoundException;
import com.bcbs239.regtech.core.infrastructure.saga.SagaStartedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class SagaManager {
    private final ISagaRepository sagaRepository;
    private final CommandDispatcher commandDispatcher;
    private final ApplicationEventPublisher eventPublisher;
    private final Supplier<Instant> currentTimeSupplier;
    private final TimeoutScheduler timeoutScheduler;
    private final ILogger logger;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> SagaId startSaga(Class<? extends AbstractSaga<T>> sagaClass, T data) {
        SagaId sagaId = SagaId.generate();
        AbstractSaga<T> saga = createSagaInstance(sagaClass, sagaId, data);

        // Handle the start event synchronously to initialize the saga
        SagaStartedEvent startEvent = new SagaStartedEvent(sagaId, saga.getSagaType(), currentTimeSupplier);
        saga.handle(startEvent);

        // Create snapshot and save
        SagaSnapshot snapshot = createSnapshot(saga);
        sagaRepository.save(snapshot);

        logger.asyncStructuredLog("SAGA_STARTED", Map.of(
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
                        logger.asyncStructuredLog("SAGA_POST_COMMIT_ACTION_FAILED", Map.of(
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
                logger.asyncStructuredLog("SAGA_POST_COMMIT_ACTION_FAILED", Map.of(
                    "sagaId", sagaId,
                    "error", e.getMessage()
                ));
            }
        }

        return sagaId;
    }

    @Transactional
    public void processEvent(SagaMessage event) {
        Maybe<SagaSnapshot> maybeSnapshot = sagaRepository.load(event.sagaId());
        if (maybeSnapshot.isEmpty()) {
            throw new SagaNotFoundException(event.sagaId());
        }

        SagaSnapshot snapshot = maybeSnapshot.getValue();
        AbstractSaga<?> saga = reconstructSaga(snapshot);

        // Let the saga process the incoming event and produce commands
        saga.handle(event);

        // Snapshot pending commands so persistence can record them without consuming the in-memory list
        List<SagaCommand> commandsSnapshot = saga.peekCommandsToDispatch();

        // Diagnostic log of snapshot
        try {
            logger.asyncStructuredLog("SAGA_COMMANDS_SNAPSHOT", Map.of(
                "sagaId", saga.getId(),
                "snapshotSize", commandsSnapshot.size()
            ));
            for (var cmd : commandsSnapshot) {
                logger.asyncStructuredLog("SAGA_COMMAND_SNAPSHOT_ITEM", Map.of(
                    "sagaId", saga.getId(),
                    "commandType", cmd.commandType()
                ));
            }
        } catch (Exception e) {
            // ignore logging errors
        }

        // Persist saga once with updated state
        Result<SagaId> saveResult;
        try {
            saveResult = sagaRepository.save(saga.toSnapshot(objectMapper));
        } catch (Exception e) {
            logger.asyncStructuredLog("SAGA_SAVE_EXCEPTION", Map.of(
                "sagaId", saga.getId(),
                "error", e.getMessage()
            ));
            throw new RuntimeException("Failed to create saga snapshot: " + e.getMessage(), e);
        }
        
        if (saveResult.isFailure()) {
            String errorMsg = saveResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error");
            logger.asyncStructuredLog("SAGA_SAVE_FAILED", Map.of(
                "sagaId", saga.getId(),
                "error", errorMsg
            ));
            throw new RuntimeException("Failed to save saga: " + errorMsg);
        }

        // Consume in-memory commands (clears the saga's command list) to avoid duplicate dispatching
        saga.getCommandsToDispatch();

        // Dispatch the snapshot of commands (these registrations will occur inside the current transaction)
        if (!commandsSnapshot.isEmpty()) {
            try {
                for (var cmd : commandsSnapshot) {
                    logger.asyncStructuredLog("SAGA_DISPATCHING_SNAPSHOT_COMMAND", Map.of(
                        "sagaId", saga.getId(),
                        "commandType", cmd.commandType()
                    ));
                    commandDispatcher.dispatch(cmd);
                }
            } catch (Exception e) {
                logger.asyncStructuredLog("SAGA_DISPATCH_SNAPSHOT_FAILED", Map.of(
                    "sagaId", saga.getId(),
                    "error", e.getMessage()
                ));
                throw e;
            }
        }

        publishSagaLifecycleEvent(saga);
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
            Constructor<?> constructor = sagaClass.getDeclaredConstructor(SagaId.class, data.getClass(), TimeoutScheduler.class, ILogger.class);
            return (AbstractSaga<T>) constructor.newInstance(sagaId, data, timeoutScheduler, logger);
        } catch (Exception e) {
            throw new SagaCreationException("Failed to create saga instance: " + sagaClass.getSimpleName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private AbstractSaga<?> reconstructSaga(SagaSnapshot snapshot) {
        try {
            // Find saga class by scanning for classes with matching sagaType
            Class<? extends AbstractSaga<?>> sagaClass = findSagaClass(snapshot.getSagaType());
            
            // Discover data class from saga constructor
            Class<?> dataClass = discoverSagaDataClass(sagaClass);
            
            // Deserialize saga data
            Object data = objectMapper.readValue(snapshot.getSagaData(), dataClass);

            // Create saga instance
            Constructor<?> constructor = sagaClass.getDeclaredConstructor(SagaId.class, dataClass, TimeoutScheduler.class, ILogger.class);
            AbstractSaga<?> saga = (AbstractSaga<?>) constructor.newInstance(snapshot.getSagaId(), data, timeoutScheduler, logger);

            // Restore saga state
            saga.setStatus(snapshot.getStatus());
            saga.setCompletedAt(snapshot.getCompletedAt());

            // TODO: Restore processed events and pending commands from snapshot

            return saga;

        } catch (Exception e) {
            throw new SagaCreationException("Failed to reconstruct saga from snapshot: " + snapshot.getSagaId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends AbstractSaga<?>> findSagaClass(String sagaType) throws ClassNotFoundException {
        // Try common package patterns
        String[] packagePrefixes = {
            "com.bcbs239.regtech.billing.application.payments.",
            "com.bcbs239.regtech.billing.application.policies.",
            "com.bcbs239.regtech.core.sagav2.",
            "com.bcbs239.regtech.core.application.saga."
        };
        
        for (String prefix : packagePrefixes) {
            try {
                return (Class<? extends AbstractSaga<?>>) Class.forName(prefix + sagaType);
            } catch (ClassNotFoundException e) {
                // Try next prefix
            }
        }
        
        throw new ClassNotFoundException("Could not find saga class for type: " + sagaType);
    }

    private Class<?> discoverSagaDataClass(Class<? extends AbstractSaga<?>> sagaClass) throws NoSuchMethodException {
        // Find constructor with signature (SagaId, T, TimeoutScheduler, ILogger)
        for (Constructor<?> constructor : sagaClass.getDeclaredConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 4 && 
                paramTypes[0] == SagaId.class && 
                paramTypes[2] == TimeoutScheduler.class &&
                paramTypes[3] == ILogger.class) {
                return paramTypes[1]; // Return the data class type
            }
        }
        throw new NoSuchMethodException("Could not find saga constructor with expected signature");
    }

    private SagaSnapshot createSnapshot(AbstractSaga<?> saga) {
        try {
            String sagaData = objectMapper.writeValueAsString(saga.getData());
            String processedEvents = objectMapper.writeValueAsString(
                saga.getProcessedEvents().stream()
                    .map(e -> e.getClass().getSimpleName())
                    .toList()
            );
            String pendingCommands = objectMapper.writeValueAsString(saga.peekCommandsToDispatch());

            return new SagaSnapshot(
                saga.getId(),
                saga.getSagaType(),
                saga.getStatus(),
                saga.getStartedAt(),
                sagaData,
                processedEvents,
                pendingCommands,
                saga.getCompletedAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create saga snapshot", e);
        }
    }
}

