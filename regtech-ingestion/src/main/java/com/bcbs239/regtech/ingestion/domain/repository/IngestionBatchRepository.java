package com.bcbs239.regtech.ingestion.domain.repository;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.ingestion.domain.model.BatchId;
import com.bcbs239.regtech.ingestion.domain.model.BatchStatus;
import com.bcbs239.regtech.ingestion.domain.model.BankId;
import com.bcbs239.regtech.ingestion.domain.model.IngestionBatch;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for IngestionBatch aggregate.
 * Defines domain operations for batch persistence.
 */
public interface IngestionBatchRepository {
    
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
     * Delete a batch (for cleanup purposes).
     */
    Result<Void> delete(BatchId batchId);
    
    /**
     * Check if a batch exists.
     */
    boolean existsByBatchId(BatchId batchId);
}