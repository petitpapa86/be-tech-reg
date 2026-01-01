package com.bcbs239.regtech.core.application.saga;

import com.bcbs239.regtech.core.domain.events.IIntegrationEventBus;
import com.bcbs239.regtech.core.domain.saga.*;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.application.commandprocessing.CommandDispatcher;
import com.bcbs239.regtech.core.domain.saga.SagaCompletedEvent;
import com.bcbs239.regtech.core.domain.saga.SagaStartedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final IIntegrationEventBus integrationEventBus;
    private final Supplier<Instant> currentTimeSupplier;
    private final TimeoutScheduler timeoutScheduler;
    private static final Logger log = LoggerFactory.getLogger(SagaManager.class);
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

        log.info("SAGA_STARTED; details={}", Map.of(
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
                        log.error("SAGA_POST_COMMIT_ACTION_FAILED; details={}", Map.of(
                            "sagaId", sagaId,
                            "error", e.getMessage()
                        ), e);
                    }
                }
            });
        } else {
            // No transaction synchronization (e.g. in unit tests) - perform post-save actions immediately
            try {
                saga.getCommandsToDispatch().forEach(commandDispatcher::dispatch);
                eventPublisher.publishEvent(startEvent);
            } catch (Exception e) {
                log.error("SAGA_POST_COMMIT_ACTION_FAILED; details={}", Map.of(
                    "sagaId", sagaId,
                    "error", e.getMessage()
                ), e);
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
            log.info("SAGA_COMMANDS_SNAPSHOT; details={}", Map.of(
                "sagaId", saga.getId(),
                "snapshotSize", commandsSnapshot.size()
            ));
            for (var cmd : commandsSnapshot) {
                log.info("SAGA_COMMAND_SNAPSHOT_ITEM; details={}", Map.of(
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
            log.error("SAGA_SAVE_EXCEPTION; details={}", Map.of(
                "sagaId", saga.getId(),
                "error", e.getMessage()
            ), e);
            throw new RuntimeException("Failed to create saga snapshot: " + e.getMessage(), e);
        }
        
        if (saveResult.isFailure()) {
            String errorMsg = saveResult.getError().map(ErrorDetail::getMessage).orElse("Unknown error");
            log.error("SAGA_SAVE_FAILED; details={}", Map.of(
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
                    log.info("SAGA_DISPATCHING_SNAPSHOT_COMMAND; details={}", Map.of(
                        "sagaId", saga.getId(),
                        "commandType", cmd.commandType()
                    ));
                    commandDispatcher.dispatch(cmd);
                }
            } catch (Exception e) {
                log.error("SAGA_DISPATCH_SNAPSHOT_FAILED; details={}", Map.of(
                    "sagaId", saga.getId(),
                    "error", e.getMessage()
                ), e);
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
            // Try 5-param constructor first (with IIntegrationEventBus)
            try {
                Constructor<?> constructor = sagaClass.getDeclaredConstructor(
                    SagaId.class, data.getClass(), TimeoutScheduler.class,
                    ApplicationEventPublisher.class, IIntegrationEventBus.class);
                return (AbstractSaga<T>) constructor.newInstance(
                    sagaId, data, timeoutScheduler, eventPublisher, integrationEventBus);
            } catch (NoSuchMethodException e) {
                // Try 4-param constructor (with ApplicationEventPublisher)
                try {
                    Constructor<?> constructor = sagaClass.getDeclaredConstructor(
                        SagaId.class, data.getClass(), TimeoutScheduler.class,
                        ApplicationEventPublisher.class);
                    return (AbstractSaga<T>) constructor.newInstance(
                        sagaId, data, timeoutScheduler, eventPublisher);
                } catch (NoSuchMethodException e2) {
                    // Fall back to 3-param constructor
                    Constructor<?> constructor = sagaClass.getDeclaredConstructor(
                        SagaId.class, data.getClass(), TimeoutScheduler.class);
                    return (AbstractSaga<T>) constructor.newInstance(
                        sagaId, data, timeoutScheduler);
                }
            }
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

            // Create saga instance - try 5-param, then 4-param, then 3-param
            AbstractSaga<?> saga;
            try {
                Constructor<?> constructor = sagaClass.getDeclaredConstructor(
                    SagaId.class, dataClass, TimeoutScheduler.class,
                    ApplicationEventPublisher.class, IIntegrationEventBus.class);
                saga = (AbstractSaga<?>) constructor.newInstance(
                    snapshot.getSagaId(), data, timeoutScheduler, eventPublisher, integrationEventBus);
            } catch (NoSuchMethodException e) {
                // Try 4-param constructor
                try {
                    Constructor<?> constructor = sagaClass.getDeclaredConstructor(
                        SagaId.class, dataClass, TimeoutScheduler.class,
                        ApplicationEventPublisher.class);
                    saga = (AbstractSaga<?>) constructor.newInstance(
                        snapshot.getSagaId(), data, timeoutScheduler, eventPublisher);
                } catch (NoSuchMethodException e2) {
                    // Fall back to 3-param constructor
                    Constructor<?> constructor = sagaClass.getDeclaredConstructor(
                        SagaId.class, dataClass, TimeoutScheduler.class);
                    saga = (AbstractSaga<?>) constructor.newInstance(
                        snapshot.getSagaId(), data, timeoutScheduler);
                }
            }

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
        // Try to find constructor with expected patterns, checking in priority order
        // Priority: 5-param > 4-param > 3-param
        
        Constructor<?> foundConstructor = null;
        int foundPriority = -1;
        
        for (Constructor<?> constructor : sagaClass.getDeclaredConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            
            // Check if first param is SagaId and third param is TimeoutScheduler
            if (paramTypes.length >= 3 && 
                paramTypes[0] == SagaId.class && 
                paramTypes[2] == TimeoutScheduler.class) {
                
                int priority = -1;
                
                // 5-param: (SagaId, T, TimeoutScheduler, ApplicationEventPublisher, IIntegrationEventBus)
                if (paramTypes.length == 5 && 
                    paramTypes[3] == ApplicationEventPublisher.class &&
                    paramTypes[4] == IIntegrationEventBus.class) {
                    priority = 3;
                }
                // 4-param: (SagaId, T, TimeoutScheduler, ApplicationEventPublisher)
                else if (paramTypes.length == 4 && 
                         paramTypes[3] == ApplicationEventPublisher.class) {
                    priority = 2;
                }
                // 3-param: (SagaId, T, TimeoutScheduler)
                else if (paramTypes.length == 3) {
                    priority = 1;
                }
                
                // Keep the highest priority constructor found
                if (priority > foundPriority) {
                    foundConstructor = constructor;
                    foundPriority = priority;
                }
            }
        }
        
        if (foundConstructor != null) {
            return foundConstructor.getParameterTypes()[1]; // Return the data class type (second parameter)
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
