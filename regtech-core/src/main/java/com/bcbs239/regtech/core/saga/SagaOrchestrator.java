package com.bcbs239.regtech.core.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Core saga orchestrator that manages distributed transactions across bounded contexts.
 * Uses functional closures for all operations to enable better testability.
 * Provides timeout management, compensation handling, and monitoring integration.
 */
@Component
public class SagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final Map<String, SagaExecution<?>> activeSagas = new ConcurrentHashMap<>();
    private final Executor sagaExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Closures for external dependencies
    private final SagaClosures.SagaDataSaver sagaDataSaver;
    private final SagaClosures.SagaDataFinder sagaDataFinder;
    private final SagaClosures.MessagePublisher messagePublisher;
    private final SagaClosures.SagaEventRecorder sagaEventRecorder;
    private final SagaClosures.SagaStepRecorder sagaStepRecorder;
    private final SagaClosures.MessageEventRecorder messageEventRecorder;
    private final SagaClosures.TimeoutScheduler timeoutScheduler;
    private final SagaClosures.TimeoutCanceler timeoutCanceler;
    private final SagaClosures.Clock clock;
    private final SagaClosures.Logger loggerClosure;

    // Constructor with concrete services (for Spring injection)
    public SagaOrchestrator(SagaRepository sagaRepository,
                           MessageBus messageBus,
                           MonitoringService monitoringService,
                           BusinessTimeoutService timeoutService) {
        this(
            sagaRepository::save,
            sagaId -> Optional.ofNullable(sagaRepository.findById(sagaId)),
            messageBus::publish,
            (eventType, sagaId, sagaType, details) -> {
                switch (eventType) {
                    case "started" -> monitoringService.recordSagaStarted(sagaId, sagaType);
                    case "completed" -> monitoringService.recordSagaCompleted(sagaId);
                    case "compensating" -> monitoringService.recordSagaCompensating(sagaId, details.length > 0 ? (String) details[0] : null);
                    case "compensated" -> monitoringService.recordSagaCompensated(sagaId);
                    case "failed" -> monitoringService.recordSagaFailed(sagaId, details.length > 0 ? (String) details[0] : null);
                }
            },
            (sagaId, stepName, success, durationMs, details) -> monitoringService.recordSagaStep(sagaId, stepName, success, durationMs),
            (sagaId, messageType, direction, source, target, details) -> {
                if ("sent".equals(direction)) {
                    monitoringService.recordMessageSent(sagaId, messageType, target);
                } else if ("received".equals(direction)) {
                    monitoringService.recordMessageReceived(sagaId, messageType, source);
                }
            },
            (sagaId, timeoutType, delayMs, callback) -> {
                timeoutService.scheduleTimeout(sagaId, timeoutType, Duration.ofMillis(delayMs), callback);
                return sagaId + "-timeout-" + timeoutType;
            },
            timeoutId -> {
                // Parse timeoutId format: "sagaId-timeout-timeoutType"
                String[] parts = timeoutId.split("-timeout-");
                if (parts.length == 2) {
                    String sagaId = parts[0];
                    String timeoutType = parts[1];
                    timeoutService.cancelTimeout(sagaId, timeoutType);
                }
            },
            Instant::now,
            (level, message, args) -> {
                String formattedMessage = String.format(message, args);
                switch (level.toLowerCase()) {
                    case "info" -> logger.info(formattedMessage);
                    case "warn" -> logger.warn(formattedMessage);
                    case "error" -> logger.error(formattedMessage);
                    case "debug" -> logger.debug(formattedMessage);
                }
            }
        );
    }

    // Constructor with closures (for testing)
    public SagaOrchestrator(
            SagaClosures.SagaDataSaver sagaDataSaver,
            SagaClosures.SagaDataFinder sagaDataFinder,
            SagaClosures.MessagePublisher messagePublisher,
            SagaClosures.SagaEventRecorder sagaEventRecorder,
            SagaClosures.SagaStepRecorder sagaStepRecorder,
            SagaClosures.MessageEventRecorder messageEventRecorder,
            SagaClosures.TimeoutScheduler timeoutScheduler,
            SagaClosures.TimeoutCanceler timeoutCanceler,
            SagaClosures.Clock clock,
            SagaClosures.Logger loggerClosure) {
        this.sagaDataSaver = sagaDataSaver;
        this.sagaDataFinder = sagaDataFinder;
        this.messagePublisher = messagePublisher;
        this.sagaEventRecorder = sagaEventRecorder;
        this.sagaStepRecorder = sagaStepRecorder;
        this.messageEventRecorder = messageEventRecorder;
        this.timeoutScheduler = timeoutScheduler;
        this.timeoutCanceler = timeoutCanceler;
        this.clock = clock;
        this.loggerClosure = loggerClosure;
    }

    /**
     * Starts a new saga execution
     */
    public <T extends SagaData> CompletableFuture<SagaResult> startSaga(Saga<T> saga, T sagaData) {
        String sagaId = sagaData.getSagaId();
        loggerClosure.log("info", "Starting saga: {}", sagaId);

        // Initialize saga data
        sagaData.setStatus(SagaData.SagaStatus.STARTED);
        sagaData.setStartedAt(clock.now());

        // Save initial state
        sagaDataSaver.save(sagaData);

        // Create execution context
        SagaExecution execution = new SagaExecution(saga, sagaData);
        activeSagas.put(sagaId, execution);

        // Start monitoring
        sagaEventRecorder.record("started", sagaId, saga.getClass().getSimpleName());

        // Execute saga asynchronously
        return CompletableFuture.supplyAsync(() -> {
            try {
                SagaResult result = saga.execute(sagaData);

                if (result.isSuccess()) {
                    completeSaga(sagaId, result);
                } else {
                    compensateSaga(sagaId, result);
                }

                return result;

            } catch (Exception e) {
                loggerClosure.log("error", "Saga execution failed: {}", sagaId);
                SagaResult failureResult = SagaResult.failure(e.getMessage());
                compensateSaga(sagaId, failureResult);
                return failureResult;
            }
        }, sagaExecutor);
    }

    /**
     * Handles incoming saga messages (events/commands from other bounded contexts)
     */
    public void handleMessage(SagaMessage message) {
        String sagaId = message.getSagaId();
        SagaExecution execution = activeSagas.get(sagaId);

        if (execution == null) {
            loggerClosure.log("warn", "Received message for unknown saga: {}", sagaId);
            return;
        }

        try {
            SagaResult result = execution.getSaga().handleMessage(execution.getSagaData(), message);

            if (result.isSuccess()) {
                updateSagaProgress(sagaId, message);
            } else {
                compensateSaga(sagaId, result);
            }

        } catch (Exception e) {
            loggerClosure.log("error", "Failed to handle saga message: {}", sagaId);
            compensateSaga(sagaId, SagaResult.failure("Message handling failed: " + e.getMessage()));
        }
    }

    /**
     * Completes a successful saga
     */
    private void completeSaga(String sagaId, SagaResult result) {
        SagaExecution execution = activeSagas.remove(sagaId);
        if (execution != null) {
            execution.getSagaData().setStatus(SagaData.SagaStatus.COMPLETED);
            execution.getSagaData().setCompletedAt(clock.now());
            sagaDataSaver.save(execution.getSagaData());

            sagaEventRecorder.record("completed", sagaId, null);
            loggerClosure.log("info", "Saga completed successfully: {}", sagaId);
        }
    }

    /**
     * Initiates compensation for a failed saga
     */
    private void compensateSaga(String sagaId, SagaResult result) {
        SagaExecution execution = activeSagas.get(sagaId);
        if (execution == null) {
            loggerClosure.log("warn", "Cannot compensate unknown saga: {}", sagaId);
            return;
        }

        loggerClosure.log("warn", "Starting compensation for saga: {}", sagaId);

        execution.getSagaData().setStatus(SagaData.SagaStatus.COMPENSATING);
        sagaDataSaver.save(execution.getSagaData());

        sagaEventRecorder.record("compensating", sagaId, null, result.getErrorMessage());

        // Execute compensation asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                SagaResult compensationResult = execution.getSaga().compensate(execution.getSagaData());

                if (compensationResult.isSuccess()) {
                    execution.getSagaData().setStatus(SagaData.SagaStatus.COMPENSATED);
                    execution.getSagaData().setCompletedAt(clock.now());
                    activeSagas.remove(sagaId);
                    sagaEventRecorder.record("compensated", sagaId, null);
                    loggerClosure.log("info", "Saga compensation completed: {}", sagaId);
                } else {
                    execution.getSagaData().setStatus(SagaData.SagaStatus.COMPENSATION_FAILED);
                    sagaEventRecorder.record("failed", sagaId, null, compensationResult.getErrorMessage());
                    loggerClosure.log("error", "Saga compensation failed: {}", sagaId);
                }

                sagaDataSaver.save(execution.getSagaData());

            } catch (Exception e) {
                loggerClosure.log("error", "Compensation execution failed: {}", sagaId);
                execution.getSagaData().setStatus(SagaData.SagaStatus.COMPENSATION_FAILED);
                sagaDataSaver.save(execution.getSagaData());
                sagaEventRecorder.record("failed", sagaId, null, e.getMessage());
            }
        }, sagaExecutor);
    }

    /**
     * Updates saga progress based on received messages
     */
    private void updateSagaProgress(String sagaId, SagaMessage message) {
        SagaExecution execution = activeSagas.get(sagaId);
        if (execution != null) {
            execution.getSagaData().addMetadata("lastMessage", message.getType());
            execution.getSagaData().addMetadata("lastMessageTime", clock.now().toString());
            sagaDataSaver.save(execution.getSagaData());
        }
    }

    /**
     * Handles saga timeouts
     */
    public void handleTimeout(String sagaId) {
        SagaExecution execution = activeSagas.get(sagaId);
        if (execution != null) {
            loggerClosure.log("warn", "Saga timeout triggered: {}", sagaId);
            compensateSaga(sagaId, SagaResult.failure("Saga timeout"));
        }
    }

    /**
     * Gets the current status of a saga
     */
    public SagaData.SagaStatus getSagaStatus(String sagaId) {
        SagaExecution execution = activeSagas.get(sagaId);
        if (execution != null) {
            return execution.getSagaData().getStatus();
        }

        // Check repository for completed sagas
        Optional<SagaData> sagaData = sagaDataFinder.findById(sagaId);
        return sagaData.map(SagaData::getStatus).orElse(null);
    }

    /**
     * Inner class to hold saga execution context
     */
    private static class SagaExecution<T extends SagaData> {
        private final Saga<T> saga;
        private final T sagaData;

        public SagaExecution(Saga<T> saga, T sagaData) {
            this.saga = saga;
            this.sagaData = sagaData;
        }

        public Saga<T> getSaga() { return saga; }
        public T getSagaData() { return sagaData; }
    }
}