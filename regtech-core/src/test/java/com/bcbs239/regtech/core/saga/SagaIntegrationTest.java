package com.bcbs239.regtech.core.saga;

import com.bcbs239.regtech.core.shared.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the complete saga orchestration system.
 * Tests the full workflow from saga creation through event processing to completion.
 */
class SagaIntegrationTest {

    private SagaManager sagaManager;
    private TestCommandDispatcher commandDispatcher;
    private TestEventPublisher eventPublisher;
    private InMemorySagaRepository sagaRepository;
    private Supplier<Instant> currentTimeSupplier;
    private SagaClosures.TimeoutScheduler timeoutScheduler;

    @BeforeEach
    void setUp() {
        commandDispatcher = new TestCommandDispatcher();
        eventPublisher = new TestEventPublisher();
        sagaRepository = new InMemorySagaRepository();
        currentTimeSupplier = () -> Instant.now();
        timeoutScheduler = new TestTimeoutScheduler();

        sagaManager = new SagaManager(
            sagaRepository.sagaSaver(),
            sagaRepository.sagaLoader(),
            commandDispatcher,
            eventPublisher,
            currentTimeSupplier,
            timeoutScheduler
        );
    }

    @Test
    void sagaLifecycle_shouldCompleteSuccessfully() {
        // Given - Create saga data
        TestSagaData sagaData = new TestSagaData("user123", "billing123", "premium");

        // When - Start the saga
        SagaId sagaId = sagaManager.startSaga(TestSaga.class, sagaData);

        // Then - Saga should be started
        assertThat(sagaId).isNotNull();
        assertThat(eventPublisher.getPublishedEvents()).hasSize(1);
        assertThat(eventPublisher.getPublishedEvents().get(0)).isInstanceOf(SagaStartedEvent.class);

        // And commands should be dispatched
        assertThat(commandDispatcher.getDispatchedCommands()).hasSize(1);
        SagaCommand command = commandDispatcher.getDispatchedCommands().get(0);
        assertThat(command.getSagaId()).isEqualTo(sagaId);
        assertThat(command.commandType()).isEqualTo("InitializeTestSaga");

        // When - Process success event
        SagaMessage successEvent = new TestEvent("ProcessingSuccessful", Instant.now(), sagaId);
        sagaManager.processEvent(successEvent);

        // Then - Saga should complete
        assertThat(eventPublisher.getPublishedEvents()).hasSize(2);
        assertThat(eventPublisher.getPublishedEvents().get(1)).isInstanceOf(SagaCompletedEvent.class);
    }

    // Test implementations

    private static class TestCommandDispatcher extends CommandDispatcher {
        private final List<SagaCommand> dispatchedCommands = new ArrayList<>();

        public TestCommandDispatcher() {
            super(null); // We don't need the real event publisher for testing
        }

        @Override
        public void dispatch(SagaCommand command) {
            dispatchedCommands.add(command);
            // Don't call super.dispatch() to avoid publishing to null eventPublisher
        }

        public List<SagaCommand> getDispatchedCommands() {
            return new ArrayList<>(dispatchedCommands);
        }
    }

    private static class TestEventPublisher implements ApplicationEventPublisher {
        private final List<Object> publishedEvents = new ArrayList<>();

        @Override
        public void publishEvent(Object event) {
            publishedEvents.add(event);
        }

        public List<Object> getPublishedEvents() {
            return new ArrayList<>(publishedEvents);
        }
    }

    private static class InMemorySagaRepository {
        private final java.util.Map<String, AbstractSaga<?>> sagas = new ConcurrentHashMap<>();

        public Function<AbstractSaga<?>, Result<SagaId>> sagaSaver() {
            return saga -> {
                sagas.put(saga.getId().id(), saga);
                return Result.success(saga.getId());
            };
        }

        public Function<SagaId, AbstractSaga<?>> sagaLoader() {
            return sagaId -> sagas.get(sagaId.id());
        }
    }

    // Test saga that simulates a complete workflow
    public static class TestSaga extends AbstractSaga<TestSagaData> {
        public TestSaga(SagaId id, TestSagaData data, SagaClosures.TimeoutScheduler timeoutScheduler) {
            super(id, "TestSaga", data, timeoutScheduler);
            dispatchCommand(new SagaCommand(getId(), "InitializeTestSaga", Map.of(), Instant.now()));

            // Register event handlers
            onEvent(TestEvent.class, event -> {
                if ("ProcessingSuccessful".equals(event.eventType())) {
                    setStatus(SagaStatus.COMPLETED);
                }
            });
        }

        @Override
        protected void updateStatus() {
            // Status is updated in event handlers
        }

        @Override
        protected void compensate() {
            // Test implementation - do nothing
        }
    }

    public static class TestSagaData {
        private final String userId;
        private final String billingAccountId;
        private final String subscriptionTier;

        public TestSagaData(String userId, String billingAccountId, String subscriptionTier) {
            this.userId = userId;
            this.billingAccountId = billingAccountId;
            this.subscriptionTier = subscriptionTier;
        }
    }

    private static class TestEvent extends SagaMessage {
        public TestEvent(String eventType, Instant occurredAt, SagaId sagaId) {
            super(eventType, occurredAt, sagaId);
        }
    }

    private static class TestTimeoutScheduler implements SagaClosures.TimeoutScheduler {
        @Override
        public void schedule(String key, long delayMillis, Runnable task) {
            // For testing, just run immediately
            task.run();
        }

        @Override
        public void cancel(String key) {
            // No-op for testing
        }
    }
}