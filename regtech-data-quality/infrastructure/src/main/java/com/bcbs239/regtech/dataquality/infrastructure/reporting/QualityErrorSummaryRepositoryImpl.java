package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.report.IQualityErrorSummaryRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityErrorSummary;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA implementation of IQualityErrorSummaryRepository.
 * Handles persistence operations for QualityErrorSummary value objects.
 */
@Repository
@Transactional
public class QualityErrorSummaryRepositoryImpl implements IQualityErrorSummaryRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityErrorSummaryRepositoryImpl.class);
    
    private final QualityErrorSummaryJpaRepository jpaRepository;
    private final QualityErrorSummaryMapper mapper;
    
    public QualityErrorSummaryRepositoryImpl(
        QualityErrorSummaryJpaRepository jpaRepository,
        QualityErrorSummaryMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Result<QualityErrorSummary> save(QualityErrorSummary errorSummary) {
        try {
            // Extract bank ID from batch ID (assuming it's part of the batch context)
            // In a real implementation, you might need to look up the bank ID from the batch
            BankId bankId = extractBankIdFromBatch(errorSummary.batchId());
            
            QualityErrorSummaryEntity entity = mapper.toEntity(errorSummary, bankId);
            QualityErrorSummaryEntity savedEntity = jpaRepository.save(entity);
            QualityErrorSummary savedSummary = mapper.toDomain(savedEntity);
            
            logger.debug("Successfully saved quality error summary for batch: {} rule: {}", 
                errorSummary.batchId().value(), errorSummary.ruleCode());
            return Result.success(savedSummary);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation saving quality error summary: batch {} rule {}", 
                errorSummary.batchId().value(), errorSummary.ruleCode(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARY_SAVE_CONSTRAINT_VIOLATION",
                "Quality error summary violates database constraints: " + e.getMessage(),
                "error_summary"
            ));
        } catch (DataAccessException e) {
            logger.error("Database error saving quality error summary: batch {} rule {}", 
                errorSummary.batchId().value(), errorSummary.ruleCode(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARY_SAVE_ERROR",
                "Failed to save quality error summary: " + e.getMessage(),
                "database"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error saving quality error summary: batch {} rule {}", 
                errorSummary.batchId().value(), errorSummary.ruleCode(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARY_SAVE_UNEXPECTED_ERROR",
                "Unexpected error saving quality error summary: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    @Override
    public Result<List<QualityErrorSummary>> saveAll(List<QualityErrorSummary> errorSummaries) {
        try {
            if (errorSummaries.isEmpty()) {
                return Result.success(List.of());
            }
            
            // Extract bank ID from the first batch (assuming all summaries are for the same batch)
            BankId bankId = extractBankIdFromBatch(errorSummaries.get(0).batchId());
            
            List<QualityErrorSummaryEntity> entities = errorSummaries.stream()
                .map(summary -> mapper.toEntity(summary, bankId))
                .collect(Collectors.toList());
            
            List<QualityErrorSummaryEntity> savedEntities = jpaRepository.saveAll(entities);
            List<QualityErrorSummary> savedSummaries = savedEntities.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
            
            logger.debug("Successfully saved {} quality error summaries", savedSummaries.size());
            return Result.success(savedSummaries);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation saving quality error summaries", e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARIES_SAVE_CONSTRAINT_VIOLATION",
                "Quality error summaries violate database constraints: " + e.getMessage(),
                "error_summaries"
            ));
        } catch (DataAccessException e) {
            logger.error("Database error saving quality error summaries", e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARIES_SAVE_ERROR",
                "Failed to save quality error summaries: " + e.getMessage(),
                "database"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error saving quality error summaries", e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARIES_SAVE_UNEXPECTED_ERROR",
                "Unexpected error saving quality error summaries: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByBatchId(BatchId batchId) {
        try {
            return jpaRepository.findByBatchId(batchId.value())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by batch ID: {}", batchId.value(), e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByBatchIdAndDimension(BatchId batchId, QualityDimension dimension) {
        try {
            return jpaRepository.findByBatchIdAndDimension(batchId.value(), dimension)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by batch ID and dimension: {} {}", 
                batchId.value(), dimension, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByBatchIdAndSeverity(BatchId batchId, ValidationError.ErrorSeverity severity) {
        try {
            return jpaRepository.findByBatchIdAndSeverity(batchId.value(), severity)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by batch ID and severity: {} {}", 
                batchId.value(), severity, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByBatchIdAndDimensionAndSeverity(
        BatchId batchId, 
        QualityDimension dimension, 
        ValidationError.ErrorSeverity severity
    ) {
        try {
            return jpaRepository.findByBatchIdAndDimensionAndSeverity(batchId.value(), dimension, severity)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by batch ID, dimension and severity: {} {} {}", 
                batchId.value(), dimension, severity, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByRuleCode(String ruleCode) {
        try {
            return jpaRepository.findByRuleCode(ruleCode)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by rule code: {}", ruleCode, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByBankId(BankId bankId) {
        try {
            return jpaRepository.findByBankId(bankId.value())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by bank ID: {}", bankId.value(), e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByBankIdAndDimension(BankId bankId, QualityDimension dimension) {
        try {
            return jpaRepository.findByBankIdAndDimension(bankId.value(), dimension)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by bank ID and dimension: {} {}", 
                bankId.value(), dimension, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByBankIdAndSeverity(BankId bankId, ValidationError.ErrorSeverity severity) {
        try {
            return jpaRepository.findByBankIdAndSeverity(bankId.value(), severity)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by bank ID and severity: {} {}", 
                bankId.value(), severity, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByCreatedAtBetween(Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findByCreatedAtBetween(startTime, endTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by creation time range: {} to {}", 
                startTime, endTime, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByBankIdAndCreatedAtBetween(BankId bankId, Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findByBankIdAndCreatedAtBetween(bankId.value(), startTime, endTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by bank ID and creation time range: {} {} to {}", 
                bankId.value(), startTime, endTime, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findMostFrequentErrors(int limit) {
        try {
            return jpaRepository.findMostFrequentErrors(limit)
                .stream()
                .limit(limit)
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding most frequent quality error summaries with limit: {}", limit, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findMostFrequentErrorsByBankId(BankId bankId, int limit) {
        try {
            return jpaRepository.findMostFrequentErrorsByBankId(bankId.value(), limit)
                .stream()
                .limit(limit)
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding most frequent quality error summaries by bank ID with limit: {} {}", 
                bankId.value(), limit, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findByDimension(QualityDimension dimension) {
        try {
            return jpaRepository.findByDimension(dimension)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by dimension: {}", dimension, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityErrorSummary> findBySeverity(ValidationError.ErrorSeverity severity) {
        try {
            return jpaRepository.findBySeverity(severity)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality error summaries by severity: {}", severity, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByBatchId(BatchId batchId) {
        try {
            return jpaRepository.countByBatchId(batchId.value());
        } catch (DataAccessException e) {
            logger.error("Error counting quality error summaries by batch ID: {}", batchId.value(), e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByBatchIdAndDimension(BatchId batchId, QualityDimension dimension) {
        try {
            return jpaRepository.countByBatchIdAndDimension(batchId.value(), dimension);
        } catch (DataAccessException e) {
            logger.error("Error counting quality error summaries by batch ID and dimension: {} {}", 
                batchId.value(), dimension, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByBatchIdAndSeverity(BatchId batchId, ValidationError.ErrorSeverity severity) {
        try {
            return jpaRepository.countByBatchIdAndSeverity(batchId.value(), severity);
        } catch (DataAccessException e) {
            logger.error("Error counting quality error summaries by batch ID and severity: {} {}", 
                batchId.value(), severity, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getTotalErrorCountByBatchId(BatchId batchId) {
        try {
            return jpaRepository.getTotalErrorCountByBatchId(batchId.value());
        } catch (DataAccessException e) {
            logger.error("Error getting total error count by batch ID: {}", batchId.value(), e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getTotalErrorCountByBatchIdAndDimension(BatchId batchId, QualityDimension dimension) {
        try {
            return jpaRepository.getTotalErrorCountByBatchIdAndDimension(batchId.value(), dimension);
        } catch (DataAccessException e) {
            logger.error("Error getting total error count by batch ID and dimension: {} {}", 
                batchId.value(), dimension, e);
            return 0;
        }
    }
    
    @Override
    public Result<Void> deleteByBatchId(BatchId batchId) {
        try {
            jpaRepository.deleteByBatchId(batchId.value());
            logger.debug("Successfully deleted quality error summaries for batch: {}", batchId.value());
            return Result.success();
            
        } catch (DataAccessException e) {
            logger.error("Database error deleting quality error summaries by batch ID: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARIES_DELETE_ERROR",
                "Failed to delete quality error summaries: " + e.getMessage(),
                "database"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error deleting quality error summaries by batch ID: {}", batchId.value(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARIES_DELETE_UNEXPECTED_ERROR",
                "Unexpected error deleting quality error summaries: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    @Override
    public Result<Long> deleteOlderThan(Instant cutoffTime) {
        try {
            long deletedCount = jpaRepository.deleteByCreatedAtBefore(cutoffTime);
            logger.debug("Successfully deleted {} quality error summaries older than: {}", deletedCount, cutoffTime);
            return Result.success(deletedCount);
            
        } catch (DataAccessException e) {
            logger.error("Database error deleting quality error summaries older than: {}", cutoffTime, e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARIES_DELETE_OLD_ERROR",
                "Failed to delete old quality error summaries: " + e.getMessage(),
                "database"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error deleting quality error summaries older than: {}", cutoffTime, e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_ERROR_SUMMARIES_DELETE_OLD_UNEXPECTED_ERROR",
                "Unexpected error deleting old quality error summaries: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByBatchId(BatchId batchId) {
        try {
            return jpaRepository.existsByBatchId(batchId.value());
        } catch (DataAccessException e) {
            logger.error("Error checking if quality error summaries exist by batch ID: {}", batchId.value(), e);
            return false;
        }
    }
    
    /**
     * Helper method to extract bank ID from batch ID.
     * In a real implementation, this would likely involve a lookup to the batch repository
     * or the bank ID would be passed as a parameter to the save methods.
     */
    private BankId extractBankIdFromBatch(BatchId batchId) {
        // For now, we'll assume the bank ID is embedded in the batch ID or use a placeholder
        // In a real implementation, you would look up the batch to get the bank ID
        // This is a simplified approach for the infrastructure implementation
        String val = batchId.value();
        String prefix = val != null && val.length() >= 8 ? val.substring(0, 8) : val;
        return new BankId("bank-" + (prefix != null ? prefix : "unknown"));
    }
}

