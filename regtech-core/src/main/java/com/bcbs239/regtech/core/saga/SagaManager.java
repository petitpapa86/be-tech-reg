package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.config.LoggingConfiguration;
import com.bcbs239.regtech.core.shared.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class SagaManager {
    private final Function<AbstractSaga<?>, Result<SagaId>> sagaSaver;
    private final Function<SagaId, AbstractSaga<?>> sagaLoader;
    private final CommandDispatcher commandDispatcher;
    private final ApplicationEventPublisher eventPublisher;
    private final Supplier<Instant> currentTimeSupplier;

    public <T> SagaId startSaga(Class<? extends AbstractSaga<T>> sagaClass, T data) {
        SagaId sagaId = SagaId.generate();
        AbstractSaga<T> saga = createSagaInstance(sagaClass, sagaId, data);

        sagaSaver.apply(saga);
        LoggingConfiguration.createStructuredLog("SAGA_STARTED", Map.of(
            "sagaId", sagaId,
            "sagaType", sagaClass.getSimpleName()
        ));

        dispatchCommands(saga);
        eventPublisher.publishEvent(new SagaStartedEvent(sagaId, saga.getSagaType(), currentTimeSupplier));

        return sagaId;
    }

    public void processEvent(SagaMessage event) {
        AbstractSaga<?> saga = sagaLoader.apply(event.getSagaId());
        if (saga == null) {
            throw new SagaNotFoundException(event.getSagaId());
        }

        saga.handle(event);
        sagaSaver.apply(saga);

        dispatchCommands(saga);
        publishSagaLifecycleEvent(saga);
    }

    private void dispatchCommands(AbstractSaga<?> saga) {
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
            Constructor<?> constructor = sagaClass.getDeclaredConstructor(SagaId.class, data.getClass());
            return (AbstractSaga<T>) constructor.newInstance(sagaId, data);
        } catch (Exception e) {
            throw new SagaCreationException("Failed to create saga instance: " + sagaClass.getSimpleName(), e);
        }
    }
}