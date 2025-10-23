package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractSagaTest {

    private TestSaga saga;
    private SagaId sagaId;

    @BeforeEach
    void setUp() {
        sagaId = SagaId.of("test-saga-id");
        saga = new TestSaga(sagaId, "TestSaga", "test-data");
    }

    @Test
    void constructor_shouldInitializeSagaCorrectly() {
        // Then
        assertThat(saga.getId()).isEqualTo(sagaId);
        assertThat(saga.getSagaType()).isEqualTo("TestSaga");
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.STARTED);
        assertThat(saga.getData()).isEqualTo("test-data");
        assertThat(saga.getStartedAt()).isNotNull();
        assertThat(saga.getCompletedAt()).isNull();
    }

    @Test
    void handle_shouldProcessRegisteredEvent() {
        // Given
        TestEvent event = new TestEvent(sagaId, Instant.now());

        // When
        saga.handle(event);

        // Then
        assertThat(saga.isEventProcessed()).isTrue();
        assertThat(saga.getProcessedEvents()).hasSize(1);
        assertThat(saga.getProcessedEvents().get(0)).isEqualTo(event);
    }

    @Test
    void handle_shouldIgnoreUnregisteredEvent() {
        // Given
        SagaStartedEvent event = new SagaStartedEvent(sagaId, "TestSaga", () -> Instant.now());

        // When
        saga.handle(event);

        // Then
        assertThat(saga.isEventProcessed()).isFalse();
        assertThat(saga.getProcessedEvents()).isEmpty();
    }

    @Test
    void handle_shouldIgnoreEventsWhenCompleted() {
        // Given
        saga.complete();
        TestEvent event = new TestEvent(sagaId, Instant.now());

        // When
        saga.handle(event);

        // Then
        assertThat(saga.isEventProcessed()).isFalse();
        assertThat(saga.getProcessedEvents()).isEmpty();
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

    @Test
    void dispatchCommand_shouldAddCommandToDispatch() {
        // Given
        TestCommand command = new TestCommand(sagaId, "test-payload");

        // When
        saga.dispatchTestCommand(command);

        // Then
        List<SagaCommand> commands = saga.getCommandsToDispatch();
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0)).isEqualTo(command);
    }

    @Test
    void getCommandsToDispatch_shouldClearCommandsAfterRetrieval() {
        // Given
        TestCommand command = new TestCommand(sagaId, "test-payload");
        saga.dispatchTestCommand(command);

        // When
        List<SagaCommand> commands1 = saga.getCommandsToDispatch();
        List<SagaCommand> commands2 = saga.getCommandsToDispatch();

        // Then
        assertThat(commands1).hasSize(1);
        assertThat(commands2).isEmpty();
    }

    @Test
    void complete_shouldSetStatusAndCompletedAt() {
        // When
        saga.complete();

        // Then
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.COMPLETED);
        assertThat(saga.getCompletedAt()).isNotNull();
    }

    @Test
    void fail_shouldSetStatusAndCompletedAt() {
        // When
        saga.fail("Test failure");

        // Then
        assertThat(saga.getStatus()).isEqualTo(SagaStatus.FAILED);
        assertThat(saga.getCompletedAt()).isNotNull();
    }

    @Test
    void hasProcessedEvent_shouldReturnTrueForProcessedEvent() {
        // Given
        TestEvent event = new TestEvent(sagaId, Instant.now());
        saga.handle(event);

        // When & Then
        assertThat(saga.hasProcessedEvent(TestEvent.class)).isTrue();
        assertThat(saga.hasProcessedEvent(SagaStartedEvent.class)).isFalse();
    }

    @Test
    void isCompleted_shouldReturnTrueForCompletedOrFailed() {
        // When not completed
        assertThat(saga.isCompleted()).isFalse();

        // When completed
        saga.complete();
        assertThat(saga.isCompleted()).isTrue();

        // Reset for failed test
        saga = new TestSaga(sagaId, "TestSaga", "test-data");
        saga.fail("Test failure");
        assertThat(saga.isCompleted()).isTrue();
    }

    // Test implementation of AbstractSaga
    private static class TestSaga extends AbstractSaga<String> {
        private boolean eventProcessed = false;

        public TestSaga(SagaId id, String sagaType, String data) {
            super(id, sagaType, data);
            onEvent(TestEvent.class, this::handleTestEvent);
        }

        private void handleTestEvent(TestEvent event) {
            eventProcessed = true;
        }

        public boolean isEventProcessed() {
            return eventProcessed;
        }

        public void dispatchTestCommand(TestCommand command) {
            dispatchCommand(command);
        }

        @Override
        protected void updateStatus() {
            // Test implementation - do nothing
        }
    }

    // Test event
    private static class TestEvent extends SagaMessage {
        public TestEvent(SagaId sagaId, Instant occurredAt) {
            super("TestEvent", occurredAt, sagaId);
        }
    }

    // Test command
    private static class TestCommand extends SagaCommand {
        public TestCommand(SagaId sagaId, String payload) {
            super(sagaId, "TestCommand", Map.of("data", payload), Instant.now());
        }
    }
}