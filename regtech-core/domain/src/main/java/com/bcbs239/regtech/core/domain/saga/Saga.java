package com.bcbs239.regtech.core.domain.saga;

import java.util.List;

/**
 * Interface for saga implementations.
 * Sagas orchestrate complex business processes with compensation capabilities.
 *
 * @param <T> The type of saga data
 */
public interface Saga<T extends SagaData> {

    /**
     * Handle an incoming message/event for this saga.
     *
     * @param message The message to handle
     */
    void handle(SagaMessage message);

    /**
     * Get the commands that need to be dispatched.
     *
     * @return List of commands to dispatch
     */
    List<SagaCommand> getCommandsToDispatch();

    /**
     * Get the saga ID.
     *
     * @return The saga ID
     */
    SagaId getId();

    /**
     * Get the saga type.
     *
     * @return The saga type
     */
    String getSagaType();

    /**
     * Get the current status of the saga.
     *
     * @return The saga status
     */
    SagaStatus getStatus();

    /**
     * Get the saga data.
     *
     * @return The saga data
     */
    T getData();
}

