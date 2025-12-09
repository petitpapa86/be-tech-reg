package com.bcbs239.regtech.ingestion.domain.batch;


import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.ingestion.domain.bankinfo.BankId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for IngestionBatch aggregate.
 * Defines domain operations for batch persistence.
 */
public interface IIngestionBatchRepository {
    
    /**
     * Find a batch by its unique batch ID.
     */
    Optional<IngestionBatch> findByBatchId(BatchId batchId);
    
    /**
     * Save or update an ingestion batch.
     */
    Result<IngestionBatch> save(IngestionBatch batch);
    
    /**
     * Find all batches for a specific bank.
     */
    List<IngestionBatch> findByBankId(BankId bankId);
    
    /**
     * Find batches by status.
     */
    List<IngestionBatch> findByStatus(BatchStatus status);
    
    /**
     * Find batches by bank ID and status.
     */
    List<IngestionBatch> findByBankIdAndStatus(BankId bankId, BatchStatus status);
    
    /**
     * Find batches uploaded within a time range.
     */
    List<IngestionBatch> findByUploadedAtBetween(Instant startTime, Instant endTime);
    
    /**
     * Find batches for a bank uploaded within a time range.
     */
    List<IngestionBatch> findByBankIdAndUploadedAtBetween(BankId bankId, Instant startTime, Instant endTime);
    
    /**
     * Count batches by status.
     */
    long countByStatus(BatchStatus status);
    
    /**
     * Count batches for a bank by status.
     */
    long countByBankIdAndStatus(BankId bankId, BatchStatus status);
    
    /**
     * Find batches that are stuck in processing (uploaded more than specified minutes ago but not completed).
     */
    List<IngestionBatch> findStuckBatches(int minutesAgo);
    
    /**
     * Find batches that are stuck in specific statuses before a cutoff time.
     */
    List<IngestionBatch> findStuckBatches(List<BatchStatus> statuses, Instant cutoffTime);
    
    /**
     * Count total batches.
     */
    long count();
    
    /**
     * Delete a batch (for cleanup purposes).
     */
    Result<Void> delete(BatchId batchId);
    
    /**
     * Check if a batch exists.
     */
    boolean existsByBatchId(BatchId batchId);
    
    /**
     * Find batches within a specific time period for compliance reporting.
     */
    List<IngestionBatch> findBatchesInPeriod(Instant startTime, Instant endTime);
}

