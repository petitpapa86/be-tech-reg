package com.bcbs239.regtech.ingestion.infrastructure.persistence;

import com.bcbs239.regtech.ingestion.domain.model.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for IngestionBatchEntity.
 */
@Repository
public interface IngestionBatchJpaRepository extends JpaRepository<IngestionBatchEntity, String> {
    
    /**
     * Find batch by batch ID.
     */
    Optional<IngestionBatchEntity> findByBatchId(String batchId);
    
    /**
     * Find all batches for a specific bank.
     */
    List<IngestionBatchEntity> findByBankId(String bankId);
    
    /**
     * Find batches by status.
     */
    List<IngestionBatchEntity> findByStatus(BatchStatus status);
    
    /**
     * Find batches by bank ID and status.
     */
    List<IngestionBatchEntity> findByBankIdAndStatus(String bankId, BatchStatus status);
    
    /**
     * Find batches uploaded within a time range.
     */
    List<IngestionBatchEntity> findByUploadedAtBetween(Instant startTime, Instant endTime);
    
    /**
     * Find batches for a bank uploaded within a time range.
     */
    List<IngestionBatchEntity> findByBankIdAndUploadedAtBetween(String bankId, Instant startTime, Instant endTime);
    
    /**
     * Count batches by status.
     */
    long countByStatus(BatchStatus status);
    
    /**
     * Count batches for a bank by status.
     */
    long countByBankIdAndStatus(String bankId, BatchStatus status);
    
    /**
     * Find batches that are stuck in processing.
     * These are batches that are not in terminal states (COMPLETED, FAILED) 
     * and were uploaded more than the specified minutes ago.
     */
    @Query("SELECT b FROM IngestionBatchEntity b WHERE " +
           "b.status NOT IN ('COMPLETED', 'FAILED') AND " +
           "b.uploadedAt < :cutoffTime")
    List<IngestionBatchEntity> findStuckBatches(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Check if a batch exists by batch ID.
     */
    boolean existsByBatchId(String batchId);
    
    /**
     * Delete batch by batch ID.
     */
    void deleteByBatchId(String batchId);
    
    /**
     * Find batches ordered by upload time (most recent first).
     */
    List<IngestionBatchEntity> findByBankIdOrderByUploadedAtDesc(String bankId);
    
    /**
     * Find recent batches for a bank (within last N hours).
     */
    @Query("SELECT b FROM IngestionBatchEntity b WHERE " +
           "b.bankId = :bankId AND b.uploadedAt > :since " +
           "ORDER BY b.uploadedAt DESC")
    List<IngestionBatchEntity> findRecentBatchesForBank(@Param("bankId") String bankId, 
                                                        @Param("since") Instant since);
    
    /**
     * Find batches that are stuck in specific statuses before a cutoff time.
     */
    @Query("SELECT b FROM IngestionBatchEntity b WHERE " +
           "b.status IN :statuses AND b.updatedAt < :cutoffTime")
    List<IngestionBatchEntity> findStuckBatchesByStatuses(@Param("statuses") List<BatchStatus> statuses, 
                                                          @Param("cutoffTime") Instant cutoffTime);
}