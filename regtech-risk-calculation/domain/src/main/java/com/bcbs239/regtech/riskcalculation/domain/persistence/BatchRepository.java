package com.bcbs239.regtech.riskcalculation.domain.persistence;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.calculation.Batch;
import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchStatus;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;

import java.util.List;

/**
 * Repository interface for Batch aggregate persistence.
 * Following DDD principles, this repository works with the Batch aggregate root
 * rather than exposing low-level persistence operations.
 * 
 * Requirements: 7.1, 7.2
 */
public interface BatchRepository {
    
    /**
     * Saves a batch aggregate.
     * The aggregate's state will be persisted, including any changes made through
     * business methods like completeCalculation() or failCalculation().
     * 
     * @param batch the batch aggregate to save
     * @return Result indicating success or failure
     */
    Result<Void> save(Batch batch);
    
    /**
     * Finds a batch by its identifier.
     * Returns Maybe.empty() if the batch doesn't exist.
     * 
     * @param batchId the batch identifier
     * @return Maybe containing the batch if found, empty otherwise
     */
    Maybe<Batch> findById(BatchId batchId);
    
    /**
     * Finds all batches with a specific status.
     * Useful for querying batches that are PROCESSING, COMPLETED, or FAILED.
     * 
     * @param status the batch status to filter by
     * @return List of batches with the specified status
     */
    List<Batch> findByStatus(BatchStatus status);
}
