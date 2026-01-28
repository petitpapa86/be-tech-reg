package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of IQualityReportRepository.
 * Handles persistence operations for QualityReport aggregates.
 */
@Repository
@Transactional
public class QualityReportRepositoryImpl implements IQualityReportRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(QualityReportRepositoryImpl.class);
    
    private final QualityReportJpaRepository jpaRepository;
    private final QualityReportMapper mapper;
    
    public QualityReportRepositoryImpl(
        QualityReportJpaRepository jpaRepository,
        QualityReportMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<QualityReport> findByReportId(QualityReportId reportId) {
        try {
            return jpaRepository.findById(reportId.value())
                .map(mapper::toDomain);
        } catch (DataAccessException e) {
            logger.error("Error finding quality report by ID: {}", reportId.value(), e);
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<QualityReport> findByBatchId(BatchId batchId) {
        try {
            return jpaRepository.findByBatchId(batchId.value())
                .map(mapper::toDomain);
        } catch (DataAccessException e) {
            logger.error("Error finding quality report by batch ID: {}", batchId.value(), e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QualityReport> findMostRecentByBankIdAndStatus(BankId bankId, QualityStatus status) {
        try {
            if (bankId == null || status == null) {
                return Optional.empty();
            }

            return jpaRepository
                .findFirstByBankIdAndStatusOrderByCreatedAtDesc(bankId.value(), status)
                .map(mapper::toDomain);
        } catch (DataAccessException e) {
            logger.error("Error finding most recent quality report by bankId+status: {}, {}", bankId != null ? bankId.value() : null, status, e);
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<QualityReport> findWithFilters(
        BankId bankId,
        QualityStatus status,
        Instant dateFrom,
        String format,
        String searchQuery,
        Pageable pageable
    ) {
        try {
            Specification<QualityReportEntity> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                
                // Bank ID Filter (Mandatory)
                predicates.add(cb.equal(root.get("bankId"), bankId.value()));
                
                // Status Filter
                if (status != null) {
                    predicates.add(cb.equal(root.get("status"), status));
                }
                
                // Date Filter (from date)
                if (dateFrom != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
                }
                
                // Format Filter
                if (format != null && !format.equals("all")) {
                    predicates.add(cb.equal(root.get("fileFormat"), format));
                }
                
                // Search Query (Filename)
                if (searchQuery != null && !searchQuery.isBlank()) {
                    String pattern = "%" + searchQuery.toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("filename")), pattern));
                }
                
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            
            return jpaRepository.findAll(spec, pageable)
                .map(mapper::toDomain);
                
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports with filters: bankId={}, status={}", 
                bankId.value(), status, e);
            return Page.empty(pageable);
        }
    }

    @Override
    public Result<QualityReport> save(QualityReport report) {
        try {
            // CRITICAL FIX: Check for existing report by batch_id, not report_id
            // The business constraint is one report per batch, not unique report_ids
            Optional<QualityReportEntity> existingByBatch = jpaRepository.findByBatchId(report.getBatchId().value());
            
            if (existingByBatch.isPresent()) {
                // Report already exists for this batch - update it (upsert) instead of returning stale data.
                // Without this, the initial IN_PROGRESS row never gets populated with scores/counts/errors.
                QualityReportEntity existingEntity = existingByBatch.get();
                updateEntityFromDomain(existingEntity, report);

                QualityReportEntity savedEntity = jpaRepository.save(existingEntity);
                QualityReport savedReport = mapper.toDomain(savedEntity);

                logger.debug("Successfully updated existing quality report: {} for batch: {}",
                    savedEntity.getReportId(), report.getBatchId().value());

                return Result.success(savedReport);
            }
            
            // No existing report for this batch - create new one
            QualityReportEntity entity = mapper.toEntity(report);
            entity.setVersion(0L);  // CRITICAL: Set version for new entities
            
            // Update all fields from domain
            updateEntityFromDomain(entity, report);
            
            // Save
            QualityReportEntity savedEntity = jpaRepository.save(entity);
            QualityReport savedReport = mapper.toDomain(savedEntity);
            
            logger.debug("Successfully created new quality report: {} for batch: {}", 
                report.getReportId().value(), report.getBatchId().value());
            
            return Result.success(savedReport);
            
        } catch (DataIntegrityViolationException e) {
            return handleDuplicateKeyOrConstraintViolation(e, report);
            
        } catch (OptimisticLockingFailureException e) {
            return handleOptimisticLockConflict(report);
            
        } catch (Exception e) {
            logger.error("Failed to save quality report: {}", report.getReportId().value(), e);
            return Result.failure("SAVE_FAILED", ErrorType.SYSTEM_ERROR, 
                "Failed to save quality report: " + e.getMessage(), "system");
        }
    }
    
    /**
     * Handle duplicate key (batch_id) or other constraint violations
     */
    private Result<QualityReport> handleDuplicateKeyOrConstraintViolation(
            DataIntegrityViolationException e, QualityReport report) {
        
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (errorMsg.contains("batch_id") || errorMsg.contains("quality_reports_batch_id_key")) {
            // Duplicate batch_id - try to find the existing report
            Optional<QualityReportEntity> existing = jpaRepository.findByBatchId(report.getBatchId().value());
            
            if (existing.isPresent()) {
                logger.debug("Duplicate batch_id - returning existing report: {} for batch: {}", 
                    existing.get().getReportId(), report.getBatchId().value());
                return Result.success(mapper.toDomain(existing.get()));
            }
            
            logger.warn("Duplicate batch_id but existing report not found: {}", 
                report.getBatchId().value());
            return Result.failure("DUPLICATE_BATCH_ID", ErrorType.VALIDATION_ERROR,
                "A quality report already exists for this batch", "batch_id");
        }
        
        logger.error("Data integrity violation: {}", report.getReportId().value(), e);
        return Result.failure("CONSTRAINT_VIOLATION", ErrorType.VALIDATION_ERROR,
            "Quality report violates database constraints", "data");
    }
    
    /**
     * Handle optimistic lock conflicts
     */
    private Result<QualityReport> handleOptimisticLockConflict(QualityReport report) {
        logger.warn("Optimistic lock conflict for quality report: {}", report.getReportId().value());
        
        // Try to get the latest version
        try {
            Optional<QualityReportEntity> latest = jpaRepository.findById(report.getReportId().value());
            if (latest.isPresent()) {
                logger.debug("Returning latest version after optimistic lock conflict: {}", 
                    report.getReportId().value());
                return Result.success(mapper.toDomain(latest.get()));
            }
        } catch (Exception ex) {
            logger.error("Failed to fetch latest after optimistic lock conflict: {}", 
                report.getReportId().value(), ex);
        }
        
        // Can't get latest - return conflict error
        return Result.failure("OPTIMISTIC_LOCK_CONFLICT", ErrorType.SYSTEM_ERROR,
            "This report was modified by another process. Please refresh and try again.", "version");
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByBankId(BankId bankId) {
        try {
            return jpaRepository.findByBankId(bankId.value())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by bank ID: {}", bankId.value(), e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByBankIdAndStatus(BankId bankId, QualityStatus status) {
        try {
            return jpaRepository.findByBankIdAndStatus(bankId.value(), status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by bank ID and status: {} {}", bankId.value(), status, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByStatus(QualityStatus status) {
        try {
            return jpaRepository.findByStatus(status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by status: {}", status, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByCreatedAtBetween(Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findByCreatedAtBetween(startTime, endTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by creation time range: {} to {}", startTime, endTime, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByBankIdAndCreatedAtBetween(BankId bankId, Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findByBankIdAndCreatedAtBetween(bankId.value(), startTime, endTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by bank ID and creation time range: {} {} to {}", 
                bankId.value(), startTime, endTime, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByOverallScoreBelow(double threshold) {
        try {
            return jpaRepository.findByOverallScoreLessThan(BigDecimal.valueOf(threshold))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by overall score below: {}", threshold, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByBankIdAndOverallScoreBelow(BankId bankId, double threshold) {
        try {
            return jpaRepository.findByBankIdAndOverallScoreLessThan(bankId.value(), BigDecimal.valueOf(threshold))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by bank ID and overall score below: {} {}", 
                bankId.value(), threshold, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByStatus(QualityStatus status) {
        try {
            return jpaRepository.countByStatus(status);
        } catch (DataAccessException e) {
            logger.error("Error counting quality reports by status: {}", status, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countByBankIdAndStatus(BankId bankId, QualityStatus status) {
        try {
            return jpaRepository.countByBankIdAndStatus(bankId.value(), status);
        } catch (DataAccessException e) {
            logger.error("Error counting quality reports by bank ID and status: {} {}", bankId.value(), status, e);
            return 0;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findStuckReports(int minutesAgo) {
        try {
            Instant cutoffTime = Instant.now().minusSeconds(minutesAgo * 60L);
            return jpaRepository.findStuckReports(cutoffTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding stuck quality reports: {} minutes ago", minutesAgo, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findStuckReports(List<QualityStatus> statuses, Instant cutoffTime) {
        try {
            return jpaRepository.findStuckReportsInStatuses(statuses, cutoffTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding stuck quality reports in statuses: {} before {}", statuses, cutoffTime, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long count() {
        try {
            return jpaRepository.count();
        } catch (DataAccessException e) {
            logger.error("Error counting total quality reports", e);
            return 0;
        }
    }
    
    @Override
    public Result<Void> delete(QualityReportId reportId) {
        try {
            if (!jpaRepository.existsById(reportId.value())) {
                return Result.failure("QUALITY_REPORT_NOT_FOUND", ErrorType.NOT_FOUND_ERROR, "Quality report not found for deletion: " + reportId.value(), "report_id");
            }
            
            jpaRepository.deleteById(reportId.value());
            logger.debug("Successfully deleted quality report: {}", reportId.value());
            return Result.success();
            
        } catch (DataAccessException e) {
            logger.error("Database error deleting quality report: {}", reportId.value(), e);
            return Result.failure("QUALITY_REPORT_DELETE_ERROR", ErrorType.SYSTEM_ERROR, "Failed to delete quality report: " + e.getMessage(), "database");
        } catch (Exception e) {
            logger.error("Unexpected error deleting quality report: {}", reportId.value(), e);
            return Result.failure("QUALITY_REPORT_DELETE_UNEXPECTED_ERROR", ErrorType.SYSTEM_ERROR, "Unexpected error deleting quality report: " + e.getMessage(), "system");
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByBatchId(BatchId batchId) {
        try {
            return jpaRepository.existsByBatchId(batchId.value());
        } catch (DataAccessException e) {
            logger.error("Error checking if quality report exists by batch ID: {}", batchId.value(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findReportsInPeriod(Instant startTime, Instant endTime) {
        try {
            return jpaRepository.findReportsInPeriod(startTime, endTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports in period: {} to {}", startTime, endTime, e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<QualityReport> findMostRecentByBankId(BankId bankId) {
        try {
            return jpaRepository.findFirstByBankIdOrderByCreatedAtDesc(bankId.value())
                .map(mapper::toDomain);
        } catch (DataAccessException e) {
            logger.error("Error finding most recent quality report by bank ID: {}", bankId.value(), e);
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findNonCompliantReports() {
        try {
            return jpaRepository.findByComplianceStatusFalse()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding non-compliant quality reports", e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findNonCompliantReportsByBankId(BankId bankId) {
        try {
            return jpaRepository.findByBankIdAndComplianceStatusFalse(bankId.value())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding non-compliant quality reports by bank ID: {}", bankId.value(), e);
            return List.of();
        }
    }
    
    /**
     * Helper method to update entity fields from domain object while preserving version.
     * This is critical for optimistic locking to work correctly.
     */
    private void updateEntityFromDomain(QualityReportEntity entity, QualityReport report) {
        // Update basic fields
        entity.setBatchId(report.getBatchId().value());
        entity.setBankId(report.getBankId().value());
        entity.setStatus(report.getStatus());
        
        // Update quality scores
        if (report.getScores() != null) {
            var scores = report.getScores();
            entity.setCompletenessScore(BigDecimal.valueOf(scores.completenessScore()));
            entity.setAccuracyScore(BigDecimal.valueOf(scores.accuracyScore()));
            entity.setConsistencyScore(BigDecimal.valueOf(scores.consistencyScore()));
            entity.setTimelinessScore(BigDecimal.valueOf(scores.timelinessScore()));
            entity.setUniquenessScore(BigDecimal.valueOf(scores.uniquenessScore()));
            entity.setValidityScore(BigDecimal.valueOf(scores.validityScore()));
            entity.setOverallScore(BigDecimal.valueOf(scores.overallScore()));
            entity.setQualityGrade(scores.grade());
        }
        
        // Update validation summary
        if (report.getValidationSummary() != null) {
            var summary = report.getValidationSummary();
            entity.setTotalExposures(summary.totalExposures());
            entity.setValidExposures(summary.validExposures());
            entity.setTotalErrors(summary.totalErrors());
            
            var byDim = summary.errorsByDimension();
            if (byDim != null) {
                entity.setCompletenessErrors(byDim.getOrDefault(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.COMPLETENESS, 0));
                entity.setAccuracyErrors(byDim.getOrDefault(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.ACCURACY, 0));
                entity.setConsistencyErrors(byDim.getOrDefault(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.CONSISTENCY, 0));
                entity.setTimelinessErrors(byDim.getOrDefault(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.TIMELINESS, 0));
                entity.setUniquenessErrors(byDim.getOrDefault(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.UNIQUENESS, 0));
                entity.setValidityErrors(byDim.getOrDefault(com.bcbs239.regtech.dataquality.domain.quality.QualityDimension.VALIDITY, 0));
            }
        }
        
        // Update S3 reference
        if (report.getDetailsReference() != null) {
            var s3ref = report.getDetailsReference();
            entity.setS3Bucket(s3ref.bucket());
            entity.setS3Key(s3ref.key());
            entity.setS3Uri(s3ref.uri());
        }
        
        // Update compliance and error info
        entity.setComplianceStatus(report.isCompliant());
        entity.setErrorMessage(report.getErrorMessage());
        
        // Update processing metadata
        entity.setProcessingStartTime(report.getProcessingStartTime());
        entity.setProcessingEndTime(report.getProcessingEndTime());
        entity.setProcessingDurationMs(report.getProcessingDurationMs());
        
        // Note: createdAt and version are NOT updated - they're managed by JPA
    }
}

