package com.bcbs239.regtech.dataquality.domain.report;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for QualityReport aggregate.
 * Defines domain operations for quality report persistence.
 */
public interface IQualityReportRepository {
    
    /**
     * Find a quality report by its unique report ID.
     */
    Optional<QualityReport> findByReportId(QualityReportId reportId);
    
    /**
     * Find a quality report by batch ID.
     * There should be only one quality report per batch.
     */
    Optional<QualityReport> findByBatchId(BatchId batchId);
    
    /**
     * Save or update a quality report.
     */
    Result<QualityReport> save(QualityReport report);
    
    /**
     * Find all quality reports for a specific bank.
     */
    List<QualityReport> findByBankId(BankId bankId);
    
    /**
     * Find quality reports by bank ID and status.
     */
    List<QualityReport> findByBankIdAndStatus(BankId bankId, QualityStatus status);
    
    /**
     * Find quality reports by status.
     */
    List<QualityReport> findByStatus(QualityStatus status);
    
    /**
     * Find quality reports created within a time range.
     */
    List<QualityReport> findByCreatedAtBetween(Instant startTime, Instant endTime);
    
    /**
     * Find quality reports for a bank created within a time range.
     */
    List<QualityReport> findByBankIdAndCreatedAtBetween(BankId bankId, Instant startTime, Instant endTime);
    
    /**
     * Find quality reports with overall scores below a threshold.
     */
    List<QualityReport> findByOverallScoreBelow(double threshold);
    
    /**
     * Find quality reports for a bank with overall scores below a threshold.
     */
    List<QualityReport> findByBankIdAndOverallScoreBelow(BankId bankId, double threshold);
    
    /**
     * Count quality reports by status.
     */
    long countByStatus(QualityStatus status);
    
    /**
     * Count quality reports for a bank by status.
     */
    long countByBankIdAndStatus(BankId bankId, QualityStatus status);
    
    /**
     * Find quality reports that are stuck in processing (created more than specified minutes ago but not completed).
     */
    List<QualityReport> findStuckReports(int minutesAgo);
    
    /**
     * Find quality reports that are stuck in specific statuses before a cutoff time.
     */
    List<QualityReport> findStuckReports(List<QualityStatus> statuses, Instant cutoffTime);
    
    /**
     * Count total quality reports.
     */
    long count();
    
    /**
     * Delete a quality report (for cleanup purposes).
     */
    Result<Void> delete(QualityReportId reportId);
    
    /**
     * Check if a quality report exists for a batch.
     */
    boolean existsByBatchId(BatchId batchId);
    
    /**
     * Find quality reports within a specific time period for compliance reporting.
     */
    List<QualityReport> findReportsInPeriod(Instant startTime, Instant endTime);
    
    /**
     * Find the most recent quality report for a bank.
     */
    Optional<QualityReport> findMostRecentByBankId(BankId bankId);

    /**
     * Find the most recent quality report for a bank in a given status.
     */
    Optional<QualityReport> findMostRecentByBankIdAndStatus(BankId bankId, QualityStatus status);
    
    /**
     * Find quality reports with dynamic filters.
     */
    Page<QualityReport> findWithFilters(
        BankId bankId,
        QualityStatus status,
        Instant dateFrom,
        String format,
        String searchQuery,
        Pageable pageable
    );

    /**
     * Find quality reports with compliance issues (non-compliant status).
     */
    List<QualityReport> findNonCompliantReports();
    
    /**
     * Find quality reports for a bank with compliance issues.
     */
    List<QualityReport> findNonCompliantReportsByBankId(BankId bankId);
}

