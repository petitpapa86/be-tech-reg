package com.bcbs239.regtech.riskcalculation.domain.calculation;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BatchSummaryId;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BatchSummary aggregate
 * Defines the contract for persisting and retrieving batch summaries
 */
public interface IBatchSummaryRepository {
    
    /**
     * Save a batch summary
     */
    Result<BatchSummary> save(BatchSummary batchSummary);
    
    /**
     * Find batch summary by its unique ID
     */
    Optional<BatchSummary> findById(BatchSummaryId batchSummaryId);
    
    /**
     * Find batch summary by batch ID
     */
    Optional<BatchSummary> findByBatchId(BatchId batchId);
    
    /**
     * Check if a batch summary exists for the given batch ID
     */
    boolean existsByBatchId(BatchId batchId);
    
    /**
     * Find all batch summaries for a specific bank
     */
    Result<List<BatchSummary>> findByBankId(BankId bankId);
    
    /**
     * Find all batch summaries with pagination
     */
    Result<List<BatchSummary>> findAll(int page, int size);
    
    /**
     * Delete a batch summary (for cleanup/testing purposes)
     */
    Result<Void> delete(BatchSummaryId batchSummaryId);
}