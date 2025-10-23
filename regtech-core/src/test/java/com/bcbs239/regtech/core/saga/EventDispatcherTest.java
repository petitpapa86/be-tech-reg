package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.context.ApplicationEvent;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class EventDispatcherTest {

    @Mock
    private SagaManager sagaManager;

    private EventDispatcher eventDispatcher;

    @BeforeEach
    void setUp() {
        eventDispatcher = new EventDispatcher(sagaManager);
    }

    @Test
    void handleEvent_shouldProcessSagaMessage() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        SagaStartedEvent sagaEvent = new SagaStartedEvent(sagaId, "TestSaga", () -> Instant.now());
        ApplicationEvent applicationEvent = new ApplicationEvent(sagaEvent) {};

        // When
        eventDispatcher.handleEvent(applicationEvent);

        // Then
        verify(sagaManager).processEvent(sagaEvent);
    }

    @Test
    void handleEvent_shouldIgnoreNonSagaMessage() {
        // Given
        ApplicationEvent applicationEvent = new ApplicationEvent("not a saga message") {};

        // When
        eventDispatcher.handleEvent(applicationEvent);

        // Then
        verifyNoInteractions(sagaManager);
    }

    @Test
    void handleEvent_shouldHandleSagaNotFoundException() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        SagaStartedEvent sagaEvent = new SagaStartedEvent(sagaId, "TestSaga", () -> Instant.now());
        ApplicationEvent applicationEvent = new ApplicationEvent(sagaEvent) {};
        doThrow(new SagaNotFoundException(sagaId)).when(sagaManager).processEvent(sagaEvent);

        // When
        eventDispatcher.handleEvent(applicationEvent);

        // Then
        verify(sagaManager).processEvent(sagaEvent);
        // Logging is called, but we can't verify it easily
    }
}