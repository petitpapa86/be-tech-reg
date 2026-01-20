package com.bcbs239.regtech.dataquality.domain.report;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for QualityErrorSummary value objects.
 * Defines operations for storing and retrieving aggregated error information.
 */
public interface IQualityErrorSummaryRepository {
    
    /**
     * Save a quality error summary.
     */
    Result<QualityErrorSummary> save(QualityErrorSummary errorSummary);
    
    /**
     * Save multiple quality error summaries in a batch operation.
     */
    Result<List<QualityErrorSummary>> saveAll(List<QualityErrorSummary> errorSummaries);
    
    /**
     * Find all error summaries for a specific batch.
     */
    List<QualityErrorSummary> findByBatchId(BatchId batchId);
    
    /**
     * Find error summaries for a batch filtered by quality dimension.
     */
    List<QualityErrorSummary> findByBatchIdAndDimension(BatchId batchId, QualityDimension dimension);
    
    /**
     * Find error summaries for a batch filtered by error severity.
     */
    List<QualityErrorSummary> findByBatchIdAndSeverity(BatchId batchId, ValidationError.ErrorSeverity severity);
    
    /**
     * Find error summaries for a batch filtered by dimension and severity.
     */
    List<QualityErrorSummary> findByBatchIdAndDimensionAndSeverity(
        BatchId batchId, 
        QualityDimension dimension, 
        ValidationError.ErrorSeverity severity
    );
    
    /**
     * Find error summaries for a specific rule code across batches.
     */
    List<QualityErrorSummary> findByRuleCode(String ruleCode);
    
    /**
     * Find error summaries for a bank across all batches.
     */
    List<QualityErrorSummary> findByBankId(BankId bankId);
    
    /**
     * Find error summaries for a bank filtered by quality dimension.
     */
    List<QualityErrorSummary> findByBankIdAndDimension(BankId bankId, QualityDimension dimension);
    
    /**
     * Find error summaries for a bank filtered by error severity.
     */
    List<QualityErrorSummary> findByBankIdAndSeverity(BankId bankId, ValidationError.ErrorSeverity severity);
    
    /**
     * Find error summaries created within a time range.
     */
    List<QualityErrorSummary> findByCreatedAtBetween(Instant startTime, Instant endTime);
    
    /**
     * Find error summaries for a bank within a time range.
     */
    List<QualityErrorSummary> findByBankIdAndCreatedAtBetween(BankId bankId, Instant startTime, Instant endTime);
    
    /**
     * Find the most frequent error summaries (highest error counts) across all batches.
     */
    List<QualityErrorSummary> findMostFrequentErrors(int limit);
    
    /**
     * Find the most frequent error summaries for a specific bank.
     */
    List<QualityErrorSummary> findMostFrequentErrorsByBankId(BankId bankId, int limit);
    
    /**
     * Find error summaries by quality dimension across all batches.
     */
    List<QualityErrorSummary> findByDimension(QualityDimension dimension);
    
    /**
     * Find error summaries by error severity across all batches.
     */
    List<QualityErrorSummary> findBySeverity(ValidationError.ErrorSeverity severity);
    
    /**
     * Count total error summaries for a batch.
     */
    long countByBatchId(BatchId batchId);
    
    /**
     * Count error summaries for a batch by dimension.
     */
    long countByBatchIdAndDimension(BatchId batchId, QualityDimension dimension);
    
    /**
     * Count error summaries for a batch by severity.
     */
    long countByBatchIdAndSeverity(BatchId batchId, ValidationError.ErrorSeverity severity);
    
    /**
     * Get total error count (sum of all errorCount fields) for a batch.
     */
    long getTotalErrorCountByBatchId(BatchId batchId);
    
    /**
     * Get total error count for a batch by dimension.
     */
    long getTotalErrorCountByBatchIdAndDimension(BatchId batchId, QualityDimension dimension);
    
    /**
     * Delete all error summaries for a batch (for cleanup purposes).
     */
    Result<Void> deleteByBatchId(BatchId batchId);
    
    /**
     * Delete error summaries older than a specified time (for data retention).
     */
    Result<Long> deleteOlderThan(Instant cutoffTime);
    
    /**
     * Check if error summaries exist for a batch.
     */
    boolean existsByBatchId(BatchId batchId);
}

