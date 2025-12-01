package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.Optional;

/**
 * Repository interface for batch summary operations.
 * Defines the contract for persisting and retrieving batch summaries.
 */
public interface IBatchSummaryRepository {
    
    /**
     * Saves a batch summary.
     *
     * @param batchId The batch identifier
     * @param summary The summary data to save
     * @return Result indicating success or error
     */
    Result<Void> save(String batchId, Object summary);
    
    /**
     * Finds a batch summary by batch ID.
     *
     * @param batchId The batch identifier
     * @return Optional containing the summary if found
     */
    Optional<Object> findByBatchId(String batchId);
    
    /**
     * Deletes a batch summary.
     *
     * @param batchId The batch identifier
     * @return Result indicating success or error
     */
    Result<Void> delete(String batchId);
}
