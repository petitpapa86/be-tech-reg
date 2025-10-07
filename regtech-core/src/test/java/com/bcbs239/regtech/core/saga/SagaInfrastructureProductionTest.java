package com.bcbs239.regtech.core.saga;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Production test for the Saga Infrastructure using closures.
 * Tests core functionality with mock closures for better testability.
 */
@DisplayName("Saga Infrastructure Production Tests with Closures")
class SagaInfrastructureProductionTest {

    // Mock closures for testing
    private final AtomicReference<SagaData> savedSagaData = new AtomicReference<>();
    private final ConcurrentHashMap<String, SagaData> sagaDataStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SagaMessage> publishedMessages = new ConcurrentHashMap<>();
    private final StringBuilder logOutput = new StringBuilder();

    // Create mock closures
    private final SagaClosures.SagaDataSaver sagaDataSaver = sagaData -> {
        savedSagaData.set(sagaData);
        sagaDataStore.put(sagaData.getSagaId(), sagaData);
    };

    private final SagaClosures.SagaDataFinder sagaDataFinder = sagaId -> Optional.ofNullable(sagaDataStore.get(sagaId));

    private final SagaClosures.MessagePublisher messagePublisher = message -> {
        publishedMessages.put(message.getSagaId() + "-" + message.getType(), message);
    };

    private final SagaClosures.SagaEventRecorder sagaEventRecorder = (eventType, sagaId, sagaType, details) -> {
        logOutput.append("SAGA_EVENT: ").append(eventType).append(" - ").append(sagaId).append("\n");
    };

    private final SagaClosures.SagaStepRecorder sagaStepRecorder = (sagaId, stepName, success, durationMs, details) -> {
        logOutput.append("SAGA_STEP: ").append(stepName).append(" - ").append(success).append(" (").append(durationMs).append("ms)\n");
    };

    private final SagaClosures.MessageEventRecorder messageEventRecorder = (sagaId, messageType, direction, source, target, details) -> {
        logOutput.append("MESSAGE: ").append(direction).append(" - ").append(messageType).append(" from ").append(source).append(" to ").append(target).append("\n");
    };

    private final SagaClosures.TimeoutScheduler timeoutScheduler = (sagaId, timeoutType, delayMs, callback) -> {
        logOutput.append("TIMEOUT_SCHEDULED: ").append(timeoutType).append(" for ").append(sagaId).append(" in ").append(delayMs).append("ms\n");
        return sagaId + "-timeout-" + timeoutType;
    };

    private final SagaClosures.TimeoutCanceler timeoutCanceler = timeoutId -> {
        logOutput.append("TIMEOUT_CANCELED: ").append(timeoutId).append("\n");
    };

    private final SagaClosures.Clock clock = () -> Instant.parse("2025-10-07T15:00:00Z");

    private final SagaClosures.Logger loggerClosure = (level, message, args) -> {
        String formattedMessage = String.format(message.replace("{}", "%s"), args);
        logOutput.append("LOG_").append(level.toUpperCase()).append(": ").append(formattedMessage).append("\n");
    };

    // Create SagaOrchestrator with mock closures
    private final SagaOrchestrator sagaOrchestrator = new SagaOrchestrator(
        sagaDataSaver, sagaDataFinder, messagePublisher, sagaEventRecorder,
        sagaStepRecorder, messageEventRecorder, timeoutScheduler, timeoutCanceler,
        clock, loggerClosure
    );

    @Test
    @DisplayName("Should initialize saga orchestrator with closures successfully")
    void shouldInitializeSagaOrchestratorWithClosures() {
        // Verify that the orchestrator was created successfully with closures
        assertThat(sagaOrchestrator).isNotNull();
    }

    @Test
    @DisplayName("Should handle basic saga data operations with closures")
    void shouldHandleBasicSagaDataOperationsWithClosures() {
        // Given
        TestSagaData sagaData = new TestSagaData();
        sagaData.setId("test-saga-123");
        sagaData.setCorrelationId("correlation-456");

        // When - Test basic operations
        sagaData.setStatus(SagaData.SagaStatus.STARTED);
        sagaData.addMetadata("testKey", "testValue");

        // Then
        assertThat(sagaData.getId()).isEqualTo("test-saga-123");
        assertThat(sagaData.getSagaId()).isEqualTo("test-saga-123");
        assertThat(sagaData.getCorrelationId()).isEqualTo("correlation-456");
        assertThat(sagaData.getStatus()).isEqualTo(SagaData.SagaStatus.STARTED);
        assertThat(sagaData.getMetadata("testKey")).isEqualTo("testValue");
        assertThat(sagaData.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should create and handle saga messages correctly with closures")
    void shouldCreateAndHandleSagaMessagesWithClosures() {
        // Given
        SagaMessage message = SagaMessage.builder()
                .sagaId("test-saga-123")
                .type("test.message")
                .source("test-source")
                .target("test-target")
                .payload(Map.of("key", "value"))
                .build();

        // When - Publish message using closure
        messagePublisher.publish(message);

        // Then
        assertThat(message.getSagaId()).isEqualTo("test-saga-123");
        assertThat(message.getType()).isEqualTo("test.message");
        assertThat(message.getSource()).isEqualTo("test-source");
        assertThat(message.getTarget()).isEqualTo("test-target");
        assertThat(message.getPayload()).isEqualTo(Map.of("key", "value"));
        assertThat(message.getTimestamp()).isNotNull();

        // Verify message was published via closure
        assertThat(publishedMessages).containsKey("test-saga-123-test.message");
        assertThat(publishedMessages.get("test-saga-123-test.message")).isEqualTo(message);
    }

    @Test
    @DisplayName("Should handle saga results correctly with closures")
    void shouldHandleSagaResultsWithClosures() {
        // Given
        SagaResult successResult = SagaResult.success();
        SagaResult successWithDataResult = SagaResult.success("test data");
        SagaResult failureResult = SagaResult.failure("test error");

        // Then
        assertThat(successResult.isSuccess()).isTrue();
        assertThat(successResult.isFailure()).isFalse();
        assertThat(successResult.getData()).isNull();
        assertThat(successResult.getErrorMessage()).isNull();

        assertThat(successWithDataResult.isSuccess()).isTrue();
        assertThat(successWithDataResult.getData()).isEqualTo("test data");

        assertThat(failureResult.isFailure()).isTrue();
        assertThat(failureResult.getErrorMessage()).isEqualTo("test error");
    }

    @Test
    @DisplayName("Should validate saga status transitions with closures")
    void shouldValidateSagaStatusTransitionsWithClosures() {
        // Given
        TestSagaData sagaData = new TestSagaData();

        // When - Test status transitions
        assertThat(sagaData.getStatus()).isEqualTo(SagaData.SagaStatus.ACTIVE);

        sagaData.setStatus(SagaData.SagaStatus.STARTED);
        assertThat(sagaData.isActive()).isTrue();

        sagaData.setStatus(SagaData.SagaStatus.COMPLETED);
        assertThat(sagaData.isActive()).isFalse();
        assertThat(sagaData.isCompleted()).isTrue();

        sagaData.setStatus(SagaData.SagaStatus.FAILED);
        assertThat(sagaData.needsCompensation()).isTrue();
    }

    @Test
    @DisplayName("Should execute saga successfully with closures")
    void shouldExecuteSagaSuccessfullyWithClosures() throws Exception {
        // Given
        TestSagaData sagaData = new TestSagaData();
        sagaData.setId("test-saga-success");
        TestSaga saga = new TestSaga();

        // When
        CompletableFuture<SagaResult> future = sagaOrchestrator.startSaga(saga, sagaData);
        SagaResult result = future.get(); // Wait for completion

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(sagaData.getStatus()).isEqualTo(SagaData.SagaStatus.COMPLETED);
        assertThat(savedSagaData.get()).isEqualTo(sagaData);

        // Verify logging via closures
        String logs = logOutput.toString();
        assertThat(logs).contains("LOG_INFO: Starting saga: test-saga-success");
        assertThat(logs).contains("SAGA_EVENT: started - test-saga-success");
        assertThat(logs).contains("SAGA_EVENT: completed - test-saga-success");
    }

    @Test
    @DisplayName("Should handle message publishing with closures")
    void shouldHandleMessagePublishingWithClosures() throws Exception {
        // Given
        TestSagaData sagaData = new TestSagaData();
        sagaData.setId("test-saga-message");
        TestSaga saga = new TestSaga();

        // When - Start saga (which should complete successfully)
        CompletableFuture<SagaResult> future = sagaOrchestrator.startSaga(saga, sagaData);
        SagaResult result = future.get(); // Wait for completion

        // Then - Verify saga completed successfully
        assertThat(result.isSuccess()).isTrue();
        assertThat(sagaData.getStatus()).isEqualTo(SagaData.SagaStatus.COMPLETED);

        // Verify that we can publish messages directly via closure
        SagaMessage message = SagaMessage.builder()
                .sagaId("direct-message")
                .type("direct.test")
                .source("test")
                .target("test-target")
                .build();

        messagePublisher.publish(message);
        assertThat(publishedMessages).containsKey("direct-message-direct.test");
    }

    @Test
    @DisplayName("Should handle saga failure and compensation with closures")
    void shouldHandleSagaFailureAndCompensationWithClosures() throws Exception {
        // Given
        TestSagaData sagaData = new TestSagaData();
        sagaData.setId("test-saga-failure");
        TestSaga failingSaga = new TestSaga() {
            @Override
            public SagaResult execute(TestSagaData sagaData) {
                throw new RuntimeException("Simulated failure");
            }
        };

        // When
        CompletableFuture<SagaResult> future = sagaOrchestrator.startSaga(failingSaga, sagaData);
        SagaResult result = future.get(); // Wait for completion

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorMessage()).isEqualTo("Simulated failure");

        // Wait a bit for async compensation to complete
        Thread.sleep(100);

        // Check that compensation was initiated (status might still be COMPENSATING or completed to COMPENSATED)
        assertThat(sagaData.getStatus()).isIn(SagaData.SagaStatus.COMPENSATING, SagaData.SagaStatus.COMPENSATED);

        // Verify compensation logging
        String logs = logOutput.toString();
        assertThat(logs).contains("SAGA_EVENT: compensating - test-saga-failure");
    }

    /**
     * Test saga implementation for closure-based testing
     */
    private static class TestSaga implements Saga<TestSagaData> {

        @Override
        public SagaResult execute(TestSagaData sagaData) {
            // Simulate publishing a message
            // In real implementation, this would be done through dependency injection
            // For testing, we just return success
            return SagaResult.success();
        }

        @Override
        public SagaResult handleMessage(TestSagaData sagaData, SagaMessage message) {
            // Handle test messages
            return SagaResult.success();
        }

        @Override
        public SagaResult compensate(TestSagaData sagaData) {
            // Simulate successful compensation
            return SagaResult.success();
        }

        @Override
        public String getSagaType() {
            return "test-saga";
        }
    }

    /**
     * Test saga data for closure-based testing
     */
    private static class TestSagaData extends SagaData {
        private String userId = "test-user-123";
        private BigDecimal amount = BigDecimal.valueOf(1000.00);
        private String approvalCode;
        private boolean compensated = false;
        private boolean timedOut = false;

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public String getApprovalCode() { return approvalCode; }
        public void setApprovalCode(String approvalCode) { this.approvalCode = approvalCode; }

        public boolean isCompensated() { return compensated; }
        public void setCompensated(boolean compensated) { this.compensated = compensated; }

        public boolean isTimedOut() { return timedOut; }
        public void setTimedOut(boolean timedOut) { this.timedOut = timedOut; }
    }
}