package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.shared.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SagaManagerTest {

    @Mock
    private Function<AbstractSaga<?>, Result<SagaId>> sagaSaver;

    @Mock
    private Function<SagaId, AbstractSaga<?>> sagaLoader;

    @Mock
    private CommandDispatcher commandDispatcher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Supplier<Instant> currentTimeSupplier;

    private SagaManager sagaManager;

    @BeforeEach
    void setUp() {
        currentTimeSupplier = () -> Instant.now();
        sagaManager = new SagaManager(sagaSaver, sagaLoader, commandDispatcher, eventPublisher, currentTimeSupplier);
    }

    @Test
    void startSaga_shouldCreateAndSaveSaga() {
        // Given
        TestSagaData data = new TestSagaData();

        // When
        SagaId sagaId = sagaManager.startSaga(TestSaga.class, data);

        // Then
        assertThat(sagaId).isNotNull();
        verify(sagaSaver).apply(any(TestSaga.class));
        verify(eventPublisher).publishEvent(any(SagaStartedEvent.class));
    }

    @Test
    void startSaga_shouldDispatchCommands() {
        // Given
        TestSagaData data = new TestSagaData();

        // When
        sagaManager.startSaga(TestSaga.class, data);

        // Then
        verify(commandDispatcher).dispatch(any(SagaCommand.class));
    }

    @Test
    void processEvent_shouldLoadAndHandleEvent() {
        // Given
        SagaId sagaId = SagaId.generate();
        TestSaga saga = mock(TestSaga.class);
        Function<SagaId, AbstractSaga<?>> loader = id -> saga;
        sagaManager = new SagaManager(sagaSaver, loader, commandDispatcher, eventPublisher, currentTimeSupplier);

        when(saga.getStatus()).thenReturn(SagaStatus.STARTED);
        when(saga.getCommandsToDispatch()).thenReturn(List.of(mock(SagaCommand.class)));

        SagaMessage event = new TestSagaMessage("test", Instant.now(), sagaId);

        // When
        sagaManager.processEvent(event);

        // Then
        verify(saga).handle(event);
        verify(sagaSaver).apply(saga);
        verify(commandDispatcher).dispatch(any(SagaCommand.class));
    }

    @Test
    void processEvent_shouldPublishCompletedEvent() {
        // Given
        SagaId sagaId = SagaId.generate();
        TestSaga saga = mock(TestSaga.class);
        Function<SagaId, AbstractSaga<?>> loader = id -> saga;
        sagaManager = new SagaManager(sagaSaver, loader, commandDispatcher, eventPublisher, currentTimeSupplier);

        when(saga.getId()).thenReturn(sagaId);
        when(saga.getSagaType()).thenReturn("TestSaga");
        when(saga.getStatus()).thenReturn(SagaStatus.COMPLETED);
        when(saga.getCommandsToDispatch()).thenReturn(List.of());

        SagaMessage event = new TestSagaMessage("test", Instant.now(), sagaId);

        // When
        sagaManager.processEvent(event);

        // Then
        verify(eventPublisher).publishEvent(any(SagaCompletedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(SagaFailedEvent.class));
    }

    @Test
    void processEvent_shouldPublishFailedEvent() {
        // Given
        SagaId sagaId = SagaId.generate();
        TestSaga saga = mock(TestSaga.class);
        Function<SagaId, AbstractSaga<?>> loader = id -> saga;
        sagaManager = new SagaManager(sagaSaver, loader, commandDispatcher, eventPublisher, currentTimeSupplier);

        when(saga.getId()).thenReturn(sagaId);
        when(saga.getSagaType()).thenReturn("TestSaga");
        when(saga.getStatus()).thenReturn(SagaStatus.FAILED);
        when(saga.getCommandsToDispatch()).thenReturn(List.of());

        SagaMessage event = new TestSagaMessage("test", Instant.now(), sagaId);

        // When
        sagaManager.processEvent(event);

        // Then
        verify(eventPublisher).publishEvent(any(SagaFailedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(SagaCompletedEvent.class));
    }

    @Test
    void processEvent_shouldThrowWhenSagaNotFound() {
        // Given
        SagaId sagaId = SagaId.generate();
        Function<SagaId, AbstractSaga<?>> loader = id -> null;
        sagaManager = new SagaManager(sagaSaver, loader, commandDispatcher, eventPublisher, currentTimeSupplier);

        SagaMessage event = new TestSagaMessage("test", Instant.now(), sagaId);

        // When & Then
        assertThatThrownBy(() -> sagaManager.processEvent(event))
            .isInstanceOf(SagaNotFoundException.class)
            .hasMessageContaining(sagaId.id());
    }

    // Test data classes
    private static class TestSagaData {
    }

    private static class TestSaga extends AbstractSaga<TestSagaData> {
        TestSaga(SagaId id, TestSagaData data) {
            super(id, "TestSaga", data);
            dispatchCommand(new SagaCommand(getId(), "test", Map.of(), Instant.now()));
        }

        TestSaga() {
            super(null, "TestSaga", null);
        }

        @Override
        protected void updateStatus() {
            // No-op for testing
        }
    }

    private static class TestSagaMessage extends SagaMessage {
        TestSagaMessage(String eventType, Instant occurredAt, SagaId sagaId) {
            super(eventType, occurredAt, sagaId);
        }
    }
}