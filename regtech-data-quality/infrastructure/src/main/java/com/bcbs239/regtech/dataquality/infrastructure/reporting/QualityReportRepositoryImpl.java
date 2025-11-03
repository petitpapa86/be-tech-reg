package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.dataquality.domain.report.IQualityReportRepository;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.dataquality.domain.report.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
            return jpaRepository.findById(reportId.getValue())
                .map(mapper::toDomain);
        } catch (DataAccessException e) {
            logger.error("Error finding quality report by ID: {}", reportId.getValue(), e);
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<QualityReport> findByBatchId(BatchId batchId) {
        try {
            return jpaRepository.findByBatchId(batchId.getValue())
                .map(mapper::toDomain);
        } catch (DataAccessException e) {
            logger.error("Error finding quality report by batch ID: {}", batchId.getValue(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public Result<QualityReport> save(QualityReport report) {
        try {
            QualityReportEntity entity = mapper.toEntity(report);
            QualityReportEntity savedEntity = jpaRepository.save(entity);
            QualityReport savedReport = mapper.toDomain(savedEntity);
            
            logger.debug("Successfully saved quality report: {}", report.getReportId().getValue());
            return Result.success(savedReport);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation saving quality report: {}", report.getReportId().getValue(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_REPORT_SAVE_CONSTRAINT_VIOLATION",
                "Quality report violates database constraints: " + e.getMessage(),
                "report_id"
            ));
        } catch (DataAccessException e) {
            logger.error("Database error saving quality report: {}", report.getReportId().getValue(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_REPORT_SAVE_ERROR",
                "Failed to save quality report: " + e.getMessage(),
                "database"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error saving quality report: {}", report.getReportId().getValue(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_REPORT_SAVE_UNEXPECTED_ERROR",
                "Unexpected error saving quality report: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByBankId(BankId bankId) {
        try {
            return jpaRepository.findByBankId(bankId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by bank ID: {}", bankId.getValue(), e);
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QualityReport> findByBankIdAndStatus(BankId bankId, QualityStatus status) {
        try {
            return jpaRepository.findByBankIdAndStatus(bankId.getValue(), status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by bank ID and status: {} {}", bankId.getValue(), status, e);
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
            return jpaRepository.findByBankIdAndCreatedAtBetween(bankId.getValue(), startTime, endTime)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by bank ID and creation time range: {} {} to {}", 
                bankId.getValue(), startTime, endTime, e);
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
            return jpaRepository.findByBankIdAndOverallScoreLessThan(bankId.getValue(), BigDecimal.valueOf(threshold))
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding quality reports by bank ID and overall score below: {} {}", 
                bankId.getValue(), threshold, e);
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
            return jpaRepository.countByBankIdAndStatus(bankId.getValue(), status);
        } catch (DataAccessException e) {
            logger.error("Error counting quality reports by bank ID and status: {} {}", bankId.getValue(), status, e);
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
            if (!jpaRepository.existsById(reportId.getValue())) {
                return Result.failure(ErrorDetail.of(
                    "QUALITY_REPORT_NOT_FOUND",
                    "Quality report not found for deletion: " + reportId.getValue(),
                    "report_id"
                ));
            }
            
            jpaRepository.deleteById(reportId.getValue());
            logger.debug("Successfully deleted quality report: {}", reportId.getValue());
            return Result.success();
            
        } catch (DataAccessException e) {
            logger.error("Database error deleting quality report: {}", reportId.getValue(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_REPORT_DELETE_ERROR",
                "Failed to delete quality report: " + e.getMessage(),
                "database"
            ));
        } catch (Exception e) {
            logger.error("Unexpected error deleting quality report: {}", reportId.getValue(), e);
            return Result.failure(ErrorDetail.of(
                "QUALITY_REPORT_DELETE_UNEXPECTED_ERROR",
                "Unexpected error deleting quality report: " + e.getMessage(),
                "system"
            ));
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByBatchId(BatchId batchId) {
        try {
            return jpaRepository.existsByBatchId(batchId.getValue());
        } catch (DataAccessException e) {
            logger.error("Error checking if quality report exists by batch ID: {}", batchId.getValue(), e);
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
            return jpaRepository.findFirstByBankIdOrderByCreatedAtDesc(bankId.getValue())
                .map(mapper::toDomain);
        } catch (DataAccessException e) {
            logger.error("Error finding most recent quality report by bank ID: {}", bankId.getValue(), e);
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
            return jpaRepository.findByBankIdAndComplianceStatusFalse(bankId.getValue())
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding non-compliant quality reports by bank ID: {}", bankId.getValue(), e);
            return List.of();
        }
    }
}