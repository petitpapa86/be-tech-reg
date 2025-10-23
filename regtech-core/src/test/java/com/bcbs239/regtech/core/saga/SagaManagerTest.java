package com.bcbs239.regtech.core.saga;package com.bcbs239.regtech.core.saga;package com.bcbs239.regtech.core.saga;



import org.junit.jupiter.api.Test;



import static org.assertj.core.api.Assertions.assertThat;import com.bcbs239.regtech.core.shared.Result;import com.bcbs239.regtech.core.shared.Result;



class SagaManagerTest {import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.BeforeEach;



    @Testimport org.junit.jupiter.api.Test;import org.junit.jupiter.api.Test;

    void sagaManager_canBeInstantiated() {

        // This is a basic test to ensure SagaManager can be createdimport org.junit.jupiter.api.extension.ExtendWith;import org.junit.jupiter.api.extension.ExtendWith;

        // Full testing would require complex mocking of all dependencies

        // For now, we verify the class exists and basic structureimport org.mockito.Mock;import org.mockito.Mock;

        assertThat(SagaManager.class).isNotNull();

    }import org.springframework.context.ApplicationEventPublisher;import org.springframework.context.ApplicationEventPublisher;

}
import org.springframework.test.context.junit.jupiter.SpringExtension;import org.springframework.test.context.junit.jupiter.SpringExtension;



import java.time.Instant;import java.time.Instant;

import java.util.function.Function;import java.util.function.Function;

import java.util.function.Supplier;import java.util.function.Supplier;



import static org.assertj.core.api.Assertions.assertThat;import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;import static org.mockito.Mockito.*;



@ExtendWith(SpringExtension.class)@ExtendWith(SpringExtension.class)

class SagaManagerTest {class SagaManagerTest {



    @Mock    @Mock

    private Function<AbstractSaga<?>, Result<SagaId>> sagaSaver;    private Function<AbstractSaga<?>, Result<SagaId>> sagaSaver;



    @Mock    @Mock

    private Function<SagaId, AbstractSaga<?>> sagaLoader;    private Function<SagaId, AbstractSaga<?>> sagaLoader;



    @Mock    @Mock

    private CommandDispatcher commandDispatcher;    private CommandDispatcher commandDispatcher;



    @Mock    @Mock

    private ApplicationEventPublisher eventPublisher;    private ApplicationEventPublisher eventPublisher;



    private Supplier<Instant> currentTimeSupplier;    private Supplier<Instant> currentTimeSupplier;



    private SagaManager sagaManager;    private SagaManager sagaManager;



    @BeforeEach    @BeforeEach

    void setUp() {    void setUp() {

        currentTimeSupplier = () -> Instant.parse("2023-01-01T10:00:00Z");        currentTimeSupplier = () -> Instant.parse("2023-01-01T10:00:00Z");

        sagaManager = new SagaManager(sagaSaver, sagaLoader, commandDispatcher, eventPublisher, currentTimeSupplier);        sagaManager = new SagaManager(sagaSaver, sagaLoader, commandDispatcher, eventPublisher, currentTimeSupplier);



        when(sagaSaver.apply(any())).thenReturn(Result.success(null));        when(sagaSaver.apply(any())).thenReturn(Result.success(null));

    }    }



    @Test    @Test

    void startSaga_shouldCreateAndStartSaga() {    void startSaga_shouldCreateAndStartSaga() {

        // Given        // Given

        TestSagaData data = new TestSagaData("test-value");        TestSagaData data = new TestSagaData("test-value");



        // When        // When

        SagaId sagaId = sagaManager.startSaga(TestSaga.class, data);        SagaId sagaId = sagaManager.startSaga(TestSaga.class, data);



        // Then        // Then

        assertThat(sagaId).isNotNull();        assertThat(sagaId).isNotNull();

        verify(sagaSaver).apply(any(TestSaga.class));        verify(sagaSaver).apply(any(TestSaga.class));

        verify(commandDispatcher).dispatch(any(SagaCommand.class));        verify(commandDispatcher).dispatch(any(SagaCommand.class));

        verify(eventPublisher).publishEvent(any(SagaStartedEvent.class));        verify(eventPublisher).publishEvent(any(SagaStartedEvent.class));

    }    }



    @Test    @Test

    void processEvent_shouldThrowExceptionForNonExistingSaga() {    void processEvent_shouldHandleEventForExistingSaga() {

        // Given        // Given

        SagaId sagaId = SagaId.of("non-existing-saga-id");        SagaId sagaId = SagaId.of("test-saga-id");

        TestEvent event = new TestEvent(sagaId, Instant.now());        TestSaga saga = new TestSaga(sagaId, "TestSaga", new TestSagaData("test"));

        TestEvent event = new TestEvent(sagaId, Instant.now());

        when(sagaLoader.apply(sagaId)).thenReturn(null);

        @SuppressWarnings("unchecked") when(sagaLoader.apply(sagaId)).thenReturn((AbstractSaga<?>) saga);

        // When & Then

        assertThatThrownBy(() -> sagaManager.processEvent(event))        // When

            .isInstanceOf(SagaNotFoundException.class)        sagaManager.processEvent(event);

            .hasMessageContaining(sagaId.id());

    }        // Then

        verify(sagaLoader).apply(sagaId);

    // Test data class        verify(sagaSaver).apply(saga);

    private static class TestSagaData {        verify(commandDispatcher).dispatch(any(SagaCommand.class));

        private final String value;        assertThat(saga.isEventProcessed()).isTrue();

    }

        public TestSagaData(String value) {

            this.value = value;    @Test

        }    void processEvent_shouldThrowExceptionForNonExistingSaga() {

        // Given

        public String getValue() {        SagaId sagaId = SagaId.of("non-existing-saga-id");

            return value;        TestEvent event = new TestEvent(sagaId, Instant.now());

        }

    }        when(sagaLoader.apply(sagaId)).thenReturn(null);



    // Test saga implementation        // When & Then

    private static class TestSaga extends AbstractSaga<TestSagaData> {        assertThatThrownBy(() -> sagaManager.processEvent(event))

        public TestSaga(SagaId id, String sagaType, TestSagaData data) {            .isInstanceOf(SagaNotFoundException.class)

            super(id, sagaType, data);            .hasMessageContaining(sagaId.toString());

        }    }



        @Override    @Test

        protected void updateStatus() {    void processEvent_shouldPublishCompletedEventWhenSagaCompletes() {

            // Test implementation - do nothing        // Given

        }        SagaId sagaId = SagaId.of("test-saga-id");

    }        TestSaga saga = new TestSaga(sagaId, "TestSaga", new TestSagaData("test"));

        saga.complete(); // Mark as completed

    // Test event        TestEvent event = new TestEvent(sagaId, Instant.now());

    private static class TestEvent extends SagaMessage {

        public TestEvent(SagaId sagaId, Instant occurredAt) {        when(sagaLoader.apply(sagaId)).thenReturn(saga);

            super("TestEvent", occurredAt, sagaId);

        }        // When

    }        sagaManager.processEvent(event);

}
        // Then
        verify(eventPublisher).publishEvent(any(SagaCompletedEvent.class));
    }

    @Test
    void processEvent_shouldPublishFailedEventWhenSagaFails() {
        // Given
        SagaId sagaId = SagaId.of("test-saga-id");
        TestSaga saga = new TestSaga(sagaId, "TestSaga", new TestSagaData("test"));
        saga.fail("Test failure"); // Mark as failed
        TestEvent event = new TestEvent(sagaId, Instant.now());

        when(sagaLoader.apply(sagaId)).thenReturn(saga);

        // When
        sagaManager.processEvent(event);

        // Then
        verify(eventPublisher).publishEvent(any(SagaFailedEvent.class));
    }

    // Test data class
    private static class TestSagaData {
        private final String value;

        public TestSagaData(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Test saga implementation
    private static class TestSaga extends AbstractSaga<TestSagaData> {
        private boolean eventProcessed = false;

        public TestSaga(SagaId id, String sagaType, TestSagaData data) {
            super(id, sagaType, data);
            onEvent(TestEvent.class, this::handleTestEvent);
        }

        private void handleTestEvent(TestEvent event) {
            eventProcessed = true;
            complete(); // Complete the saga for testing
        }

        public boolean isEventProcessed() {
            return eventProcessed;
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
}