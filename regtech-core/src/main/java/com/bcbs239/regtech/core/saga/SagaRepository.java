package com.bcbs239.regtech.core.saga;

/**
 * Repository interface for saga data persistence.
 * Provides CRUD operations for saga state management.
 */
public interface SagaRepository {

    /**
     * Saves saga data to persistent storage
     */
    <T extends SagaData> void save(T sagaData);

    /**
     * Finds saga data by ID
     */
    SagaData findById(String sagaId);

    /**
     * Finds saga data by type and correlation ID
     */
    SagaData findByTypeAndCorrelationId(String sagaType, String correlationId);

    /**
     * Updates the status of a saga
     */
    void updateStatus(String sagaId, SagaData.SagaStatus status);

    /**
     * Deletes saga data (typically after successful completion)
     */
    void delete(String sagaId);

    /**
     * Finds all active sagas (for recovery purposes)
     */
    Iterable<SagaData> findActiveSagas();
}