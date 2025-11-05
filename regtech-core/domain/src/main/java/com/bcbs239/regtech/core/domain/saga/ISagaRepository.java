package com.bcbs239.regtech.core.domain.saga;

import com.bcbs239.regtech.core.domain.core.Maybe;
import com.bcbs239.regtech.core.domain.core.Result;

/**
 * Domain interface for saga repository operations.
 * Provides abstraction over saga persistence infrastructure for clean architecture compliance.
 */
public interface ISagaRepository {

    /**
     * Save a saga snapshot to persistent storage.
     * @param snapshot the saga snapshot to save
     * @return Result containing the saga ID if successful, or an error if failed
     */
    Result<SagaId> save(SagaSnapshot snapshot);

    /**
     * Load a saga snapshot from persistent storage by its ID.
     * @param sagaId the ID of the saga to load
     * @return Maybe containing the saga snapshot if found, or none if not found
     */
    Maybe<SagaSnapshot> load(SagaId sagaId);
}