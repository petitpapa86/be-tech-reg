package com.bcbs239.regtech.ingestion.infrastructure.batchtracking.persistence;

import com.bcbs239.regtech.ingestion.domain.batch.BatchStatus;
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
    
    /**
     * Find batches with pagination for large result sets.
     */
    @Query("SELECT b FROM IngestionBatchEntity b WHERE " +
           "b.bankId = :bankId ORDER BY b.uploadedAt DESC")
    List<IngestionBatchEntity> findByBankIdWithPagination(@Param("bankId") String bankId, 
                                                          org.springframework.data.domain.Pageable pageable);
    
    /**
     * Find batches by status with pagination for performance.
     */
    @Query("SELECT b FROM IngestionBatchEntity b WHERE " +
           "b.status = :status ORDER BY b.uploadedAt DESC")
    List<IngestionBatchEntity> findByStatusWithPagination(@Param("status") BatchStatus status,
                                                          org.springframework.data.domain.Pageable pageable);
    
    /**
     * Get processing performance statistics.
     */
    @Query("SELECT " +
           "AVG(b.processingDurationMs) as avgDuration, " +
           "MIN(b.processingDurationMs) as minDuration, " +
           "MAX(b.processingDurationMs) as maxDuration, " +
           "COUNT(b) as totalCount " +
           "FROM IngestionBatchEntity b WHERE " +
           "b.status = 'COMPLETED' AND b.processingDurationMs IS NOT NULL AND " +
           "b.uploadedAt BETWEEN :startTime AND :endTime")
    Object[] getProcessingStatistics(@Param("startTime") Instant startTime, 
                                   @Param("endTime") Instant endTime);
    
    /**
     * Find large files for performance analysis.
     */
    @Query("SELECT b FROM IngestionBatchEntity b WHERE " +
           "b.fileSizeBytes > :sizeThreshold ORDER BY b.fileSizeBytes DESC")
    List<IngestionBatchEntity> findLargeFiles(@Param("sizeThreshold") long sizeThreshold);
    
    /**
     * Get file size distribution statistics.
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN b.fileSizeBytes < 1048576 THEN 1 END) as small, " +
           "COUNT(CASE WHEN b.fileSizeBytes BETWEEN 1048576 AND 104857600 THEN 1 END) as medium, " +
           "COUNT(CASE WHEN b.fileSizeBytes > 104857600 THEN 1 END) as large " +
           "FROM IngestionBatchEntity b WHERE " +
           "b.uploadedAt BETWEEN :startTime AND :endTime")
    Object[] getFileSizeDistribution(@Param("startTime") Instant startTime, 
                                   @Param("endTime") Instant endTime);
    
    /**
     * Find batches with slow processing for performance monitoring.
     */
    @Query("SELECT b FROM IngestionBatchEntity b WHERE " +
           "b.processingDurationMs > :durationThreshold AND " +
           "b.status = 'COMPLETED' " +
           "ORDER BY b.processingDurationMs DESC")
    List<IngestionBatchEntity> findSlowProcessingBatches(@Param("durationThreshold") long durationThreshold);
    
    /**
     * Count active processing batches for load monitoring.
     */
    @Query("SELECT COUNT(b) FROM IngestionBatchEntity b WHERE " +
           "b.status IN ('PARSING', 'VALIDATED', 'STORING')")
    long countActiveProcessingBatches();
}



