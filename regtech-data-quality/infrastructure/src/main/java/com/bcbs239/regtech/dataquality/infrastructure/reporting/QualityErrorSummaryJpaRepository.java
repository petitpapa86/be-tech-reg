package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for QualityErrorSummaryEntity.
 * Provides database access methods for quality error summaries.
 */
@Repository
public interface QualityErrorSummaryJpaRepository extends JpaRepository<QualityErrorSummaryEntity, Long> {
    
    /**
     * Find all error summaries for a specific batch.
     */
    List<QualityErrorSummaryEntity> findByBatchId(String batchId);
    
    /**
     * Find error summaries for a batch filtered by quality dimension.
     */
    List<QualityErrorSummaryEntity> findByBatchIdAndDimension(String batchId, QualityDimension dimension);
    
    /**
     * Find error summaries for a batch filtered by error severity.
     */
    List<QualityErrorSummaryEntity> findByBatchIdAndSeverity(String batchId, ValidationError.ErrorSeverity severity);
    
    /**
     * Find error summaries for a batch filtered by dimension and severity.
     */
    List<QualityErrorSummaryEntity> findByBatchIdAndDimensionAndSeverity(
        String batchId, 
        QualityDimension dimension, 
        ValidationError.ErrorSeverity severity
    );
    
    /**
     * Find error summaries for a specific rule code across batches.
     */
    List<QualityErrorSummaryEntity> findByRuleCode(String ruleCode);
    
    /**
     * Find error summaries for a bank across all batches.
     */
    List<QualityErrorSummaryEntity> findByBankId(String bankId);
    
    /**
     * Find error summaries for a bank filtered by quality dimension.
     */
    List<QualityErrorSummaryEntity> findByBankIdAndDimension(String bankId, QualityDimension dimension);
    
    /**
     * Find error summaries for a bank filtered by error severity.
     */
    List<QualityErrorSummaryEntity> findByBankIdAndSeverity(String bankId, ValidationError.ErrorSeverity severity);
    
    /**
     * Find error summaries created within a time range.
     */
    List<QualityErrorSummaryEntity> findByCreatedAtBetween(Instant startTime, Instant endTime);
    
    /**
     * Find error summaries for a bank within a time range.
     */
    List<QualityErrorSummaryEntity> findByBankIdAndCreatedAtBetween(String bankId, Instant startTime, Instant endTime);
    
    /**
     * Find the most frequent error summaries (highest error counts) across all batches.
     */
    @Query("SELECT es FROM QualityErrorSummaryEntity es ORDER BY es.errorCount DESC")
    List<QualityErrorSummaryEntity> findMostFrequentErrors(@Param("limit") int limit);
    
    /**
     * Find the most frequent error summaries for a specific bank.
     */
    @Query("SELECT es FROM QualityErrorSummaryEntity es WHERE es.bankId = :bankId ORDER BY es.errorCount DESC")
    List<QualityErrorSummaryEntity> findMostFrequentErrorsByBankId(@Param("bankId") String bankId, @Param("limit") int limit);
    
    /**
     * Find error summaries by quality dimension across all batches.
     */
    List<QualityErrorSummaryEntity> findByDimension(QualityDimension dimension);
    
    /**
     * Find error summaries by error severity across all batches.
     */
    List<QualityErrorSummaryEntity> findBySeverity(ValidationError.ErrorSeverity severity);
    
    /**
     * Count total error summaries for a batch.
     */
    long countByBatchId(String batchId);
    
    /**
     * Count error summaries for a batch by dimension.
     */
    long countByBatchIdAndDimension(String batchId, QualityDimension dimension);
    
    /**
     * Count error summaries for a batch by severity.
     */
    long countByBatchIdAndSeverity(String batchId, ValidationError.ErrorSeverity severity);
    
    /**
     * Get total error count (sum of all errorCount fields) for a batch.
     */
    @Query("SELECT COALESCE(SUM(es.errorCount), 0) FROM QualityErrorSummaryEntity es WHERE es.batchId = :batchId")
    long getTotalErrorCountByBatchId(@Param("batchId") String batchId);
    
    /**
     * Get total error count for a batch by dimension.
     */
    @Query("SELECT COALESCE(SUM(es.errorCount), 0) FROM QualityErrorSummaryEntity es WHERE " +
           "es.batchId = :batchId AND es.dimension = :dimension")
    long getTotalErrorCountByBatchIdAndDimension(@Param("batchId") String batchId, @Param("dimension") QualityDimension dimension);
    
    /**
     * Delete all error summaries for a batch (for cleanup purposes).
     */
    @Modifying
    @Query("DELETE FROM QualityErrorSummaryEntity es WHERE es.batchId = :batchId")
    void deleteByBatchId(@Param("batchId") String batchId);
    
    /**
     * Delete error summaries older than a specified time (for data retention).
     */
    @Modifying
    @Query("DELETE FROM QualityErrorSummaryEntity es WHERE es.createdAt < :cutoffTime")
    long deleteByCreatedAtBefore(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Check if error summaries exist for a batch.
     */
    boolean existsByBatchId(String batchId);
    
    /**
     * Find error summaries ordered by error count (descending).
     */
    List<QualityErrorSummaryEntity> findAllByOrderByErrorCountDesc();
    
    /**
     * Find error summaries for a batch ordered by error count (descending).
     */
    List<QualityErrorSummaryEntity> findByBatchIdOrderByErrorCountDesc(String batchId);
    
    /**
     * Find error summaries for a bank ordered by error count (descending).
     */
    List<QualityErrorSummaryEntity> findByBankIdOrderByErrorCountDesc(String bankId);
    
    /**
     * Find error summaries by rule code and dimension.
     */
    List<QualityErrorSummaryEntity> findByRuleCodeAndDimension(String ruleCode, QualityDimension dimension);
    
    /**
     * Find error summaries by field name.
     */
    List<QualityErrorSummaryEntity> findByFieldName(String fieldName);
    
    /**
     * Find error summaries by field name for a specific batch.
     */
    List<QualityErrorSummaryEntity> findByBatchIdAndFieldName(String batchId, String fieldName);
    
    /**
     * Get distinct rule codes for a batch.
     */
    @Query("SELECT DISTINCT es.ruleCode FROM QualityErrorSummaryEntity es WHERE es.batchId = :batchId")
    List<String> findDistinctRuleCodesByBatchId(@Param("batchId") String batchId);
    
    /**
     * Get distinct field names for a batch.
     */
    @Query("SELECT DISTINCT es.fieldName FROM QualityErrorSummaryEntity es WHERE " +
           "es.batchId = :batchId AND es.fieldName IS NOT NULL")
    List<String> findDistinctFieldNamesByBatchId(@Param("batchId") String batchId);
    
    /**
     * Get error count statistics by dimension for a batch.
     */
    @Query("SELECT es.dimension, SUM(es.errorCount) FROM QualityErrorSummaryEntity es WHERE " +
           "es.batchId = :batchId GROUP BY es.dimension")
    List<Object[]> getErrorCountByDimensionForBatch(@Param("batchId") String batchId);
    
    /**
     * Get error count statistics by severity for a batch.
     */
    @Query("SELECT es.severity, SUM(es.errorCount) FROM QualityErrorSummaryEntity es WHERE " +
           "es.batchId = :batchId GROUP BY es.severity")
    List<Object[]> getErrorCountBySeverityForBatch(@Param("batchId") String batchId);
}

