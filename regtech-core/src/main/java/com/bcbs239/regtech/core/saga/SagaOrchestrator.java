package com.bcbs239.regtech.core.saga;

import java.util.concurrent.CompletableFuture;

/**
 * Orchestrator for managing saga execution.
 * Handles the lifecycle of sagas including creation, execution, and monitoring.
 */
public interface SagaOrchestrator {

    /**
     * Start a new saga instance.
     *
     * @param saga The saga to start
     * @return A future that completes when the saga is started
     */
    CompletableFuture<SagaResult> startSaga(Saga<?> saga);

    /**
     * Handle a message for an existing saga.
     *
     * @param sagaId The saga ID
     * @param message The message to handle
     * @return A future that completes when the message is processed
     */
    CompletableFuture<SagaResult> handleMessage(SagaId sagaId, SagaMessage message);

    /**
     * Get the status of a saga.
     *
     * @param sagaId The saga ID
     * @return The saga status, or null if not found
     */
    SagaStatus getSagaStatus(SagaId sagaId);
}