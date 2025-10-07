package com.bcbs239.regtech.core.saga;

/**
 * Core saga interface that defines the contract for saga implementations.
 * Sagas coordinate distributed transactions across bounded contexts.
 */
public interface Saga<T extends SagaData> {

    /**
     * Executes the main saga logic
     */
    SagaResult execute(T sagaData);

    /**
     * Handles incoming messages from other bounded contexts
     */
    SagaResult handleMessage(T sagaData, SagaMessage message);

    /**
     * Performs compensation when saga fails
     */
    SagaResult compensate(T sagaData);

    /**
     * Returns the saga type identifier
     */
    String getSagaType();
}