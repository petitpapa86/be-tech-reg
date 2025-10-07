package com.bcbs239.regtech.core.saga;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Functional interfaces defining closures for saga infrastructure operations.
 * Using closures enables better testability by allowing injection of mock functions
 * instead of concrete implementations.
 */
public interface SagaClosures {

    // ===== CLOCK CLOSURES =====

    /**
     * Closure for getting the current time.
     * Allows mocking time in tests for deterministic behavior.
     */
    @FunctionalInterface
    interface Clock {
        Instant now();
    }

    // ===== IO CLOSURES =====

    /**
     * Closure for publishing messages to the message bus.
     * IO operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface MessagePublisher {
        void publish(SagaMessage message);
    }

    /**
     * Closure for subscribing to messages on the message bus.
     * IO operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface MessageSubscriber {
        void subscribe(String messageType, Consumer<SagaMessage> handler);
    }

    /**
     * Closure for logging operations.
     * IO operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface Logger {
        void log(String level, String message, Object... args);
    }

    // ===== DATABASE CLOSURES =====

    /**
     * Closure for saving saga data to the database.
     * Database operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaDataSaver {
        void save(SagaData sagaData);
    }

    /**
     * Closure for finding saga data by ID.
     * Database operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaDataFinder {
        Optional<SagaData> findById(String sagaId);
    }

    /**
     * Closure for finding saga data by correlation ID.
     * Database operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaDataFinderByCorrelationId {
        Optional<SagaData> findByCorrelationId(String correlationId);
    }

    // ===== MONITORING CLOSURES =====

    /**
     * Closure for recording saga lifecycle events.
     * Monitoring operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaEventRecorder {
        void record(String eventType, String sagaId, String sagaType, Object... details);
    }

    /**
     * Closure for recording saga step execution.
     * Monitoring operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaStepRecorder {
        void recordStep(String sagaId, String stepName, boolean success, long durationMs, Object... details);
    }

    /**
     * Closure for recording message events.
     * Monitoring operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface MessageEventRecorder {
        void recordMessage(String sagaId, String messageType, String direction, String source, String target, Object... details);
    }

    // ===== TIMEOUT CLOSURES =====

    /**
     * Closure for scheduling business timeouts.
     * System operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface TimeoutScheduler {
        String schedule(String sagaId, String timeoutType, long delayMs, Runnable callback);
    }

    /**
     * Closure for canceling scheduled timeouts.
     * System operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface TimeoutCanceler {
        void cancel(String timeoutId);
    }

    // ===== BUSINESS LOGIC CLOSURES =====

    /**
     * Closure for executing saga business logic.
     * Business operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaExecutor<T extends SagaData> {
        SagaResult execute(T sagaData);
    }

    /**
     * Closure for handling messages in sagas.
     * Business operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaMessageHandler<T extends SagaData> {
        SagaResult handleMessage(T sagaData, SagaMessage message);
    }

    /**
     * Closure for compensating failed sagas.
     * Business operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaCompensator<T extends SagaData> {
        SagaResult compensate(T sagaData);
    }

    // ===== UTILITY CLOSURES =====

    /**
     * Closure for generating unique IDs.
     * Utility operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface IdGenerator {
        String generate();
    }

    /**
     * Closure for validating saga data.
     * Validation operation that can be mocked in tests.
     */
    @FunctionalInterface
    interface SagaDataValidator<T extends SagaData> {
        ValidationResult validate(T sagaData);
    }

    /**
     * Result of validation operations.
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}