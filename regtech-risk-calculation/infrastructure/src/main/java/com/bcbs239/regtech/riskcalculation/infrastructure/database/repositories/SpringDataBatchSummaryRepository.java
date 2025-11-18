package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchSummaryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for BatchSummaryEntity
 */
@Repository
public interface SpringDataBatchSummaryRepository extends JpaRepository<BatchSummaryEntity, Long> {

    /**
     * Find batch summary by batch summary ID
     */
    Optional<BatchSummaryEntity> findByBatchSummaryId(String batchSummaryId);

    /**
     * Find batch summary by batch ID
     */
    Optional<BatchSummaryEntity> findByBatchId(String batchId);

    /**
     * Check if batch summary exists by batch ID
     */
    boolean existsByBatchId(String batchId);

    /**
     * Find all batch summaries for a specific bank, ordered by creation date descending
     */
    @Query("SELECT bs FROM BatchSummaryEntity bs WHERE bs.bankId = :bankId ORDER BY bs.createdAt DESC")
    List<BatchSummaryEntity> findByBankIdOrderByCreatedAtDesc(@Param("bankId") String bankId);

    /**
     * Find all batch summaries with pagination, ordered by creation date descending
     */
    Page<BatchSummaryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Delete batch summary by batch summary ID
     */
    void deleteByBatchSummaryId(String batchSummaryId);
}