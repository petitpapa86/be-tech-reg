package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventDispatcherTest {

    static class SagaManagerStub extends SagaManager {
        private final List<SagaMessage> processed = new ArrayList<>();

        // Provide a no-op constructor by calling super with stubs
        public SagaManagerStub() {
            super(saga -> com.bcbs239.regtech.core.shared.Result.success(SagaId.generate()), id -> com.bcbs239.regtech.core.shared.Maybe.none(), new CommandDispatcher(event -> {}), event -> {}, java.time.Instant::now, SagaClosures.timeoutScheduler(java.util.concurrent.Executors.newSingleThreadScheduledExecutor()));
        }

        @Override
        public void processEvent(SagaMessage event) {
            processed.add(event);
            // no-op
        }

        public List<SagaMessage> getProcessed() { return processed; }
    }

    private SagaManagerStub sagaManager;
    private EventDispatcher eventDispatcher;

    @BeforeEach
    void setUp() {
        sagaManager = new SagaManagerStub();
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
        assertThat(sagaManager.getProcessed()).containsExactly(sagaEvent);
    }

    @Test
    void handleEvent_shouldIgnoreNonSagaMessage() {
        // Given
        ApplicationEvent applicationEvent = new ApplicationEvent("not a saga message") {};

        // When
        eventDispatcher.handleEvent(applicationEvent);

        // Then
        assertThat(sagaManager.getProcessed()).isEmpty();
    }

    @Test
    void handleEvent_shouldHandleSagaNotFoundException() {
        // Given: stub that throws when processing
        SagaManager throwingManager = new SagaManagerStub() {
            @Override
            public void processEvent(SagaMessage event) {
                throw new SagaNotFoundException(event.getSagaId());
            }
        };
        EventDispatcher dispatcher = new EventDispatcher(throwingManager);

        SagaId sagaId = SagaId.of("test-saga-id");
        SagaStartedEvent sagaEvent = new SagaStartedEvent(sagaId, "TestSaga", () -> Instant.now());
        ApplicationEvent applicationEvent = new ApplicationEvent(sagaEvent) {};

        // When (should not throw)
        dispatcher.handleEvent(applicationEvent);

        // Then: no exception thrown and we simply return
    }
}