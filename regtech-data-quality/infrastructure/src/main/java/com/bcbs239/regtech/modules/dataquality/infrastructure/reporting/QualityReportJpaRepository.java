package com.bcbs239.regtech.modules.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.modules.dataquality.domain.report.QualityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for QualityReportEntity.
 * Provides database access methods for quality reports.
 */
@Repository
public interface QualityReportJpaRepository extends JpaRepository<QualityReportEntity, String> {
    
    /**
     * Find a quality report by batch ID.
     */
    Optional<QualityReportEntity> findByBatchId(String batchId);
    
    /**
     * Find all quality reports for a specific bank.
     */
    List<QualityReportEntity> findByBankId(String bankId);
    
    /**
     * Find quality reports by bank ID and status.
     */
    List<QualityReportEntity> findByBankIdAndStatus(String bankId, QualityStatus status);
    
    /**
     * Find quality reports by status.
     */
    List<QualityReportEntity> findByStatus(QualityStatus status);
    
    /**
     * Find quality reports created within a time range.
     */
    List<QualityReportEntity> findByCreatedAtBetween(Instant startTime, Instant endTime);
    
    /**
     * Find quality reports for a bank created within a time range.
     */
    List<QualityReportEntity> findByBankIdAndCreatedAtBetween(String bankId, Instant startTime, Instant endTime);
    
    /**
     * Find quality reports with overall scores below a threshold.
     */
    List<QualityReportEntity> findByOverallScoreLessThan(BigDecimal threshold);
    
    /**
     * Find quality reports for a bank with overall scores below a threshold.
     */
    List<QualityReportEntity> findByBankIdAndOverallScoreLessThan(String bankId, BigDecimal threshold);
    
    /**
     * Count quality reports by status.
     */
    long countByStatus(QualityStatus status);
    
    /**
     * Count quality reports for a bank by status.
     */
    long countByBankIdAndStatus(String bankId, QualityStatus status);
    
    /**
     * Find quality reports that are stuck in processing.
     * Reports created more than specified minutes ago but not in COMPLETED or FAILED status.
     */
    @Query("SELECT qr FROM QualityReportEntity qr WHERE " +
           "qr.createdAt < :cutoffTime AND " +
           "qr.status NOT IN ('COMPLETED', 'FAILED')")
    List<QualityReportEntity> findStuckReports(@Param("cutoffTime") Instant cutoffTime);
    
    /**
     * Find quality reports that are stuck in specific statuses before a cutoff time.
     */
    @Query("SELECT qr FROM QualityReportEntity qr WHERE " +
           "qr.status IN :statuses AND " +
           "qr.createdAt < :cutoffTime")
    List<QualityReportEntity> findStuckReportsInStatuses(
        @Param("statuses") List<QualityStatus> statuses,
        @Param("cutoffTime") Instant cutoffTime
    );
    
    /**
     * Check if a quality report exists for a batch.
     */
    boolean existsByBatchId(String batchId);
    
    /**
     * Find quality reports within a specific time period for compliance reporting.
     */
    @Query("SELECT qr FROM QualityReportEntity qr WHERE " +
           "qr.createdAt >= :startTime AND qr.createdAt <= :endTime " +
           "ORDER BY qr.createdAt DESC")
    List<QualityReportEntity> findReportsInPeriod(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
    
    /**
     * Find the most recent quality report for a bank.
     */
    Optional<QualityReportEntity> findFirstByBankIdOrderByCreatedAtDesc(String bankId);
    
    /**
     * Find quality reports with compliance issues (non-compliant status).
     */
    List<QualityReportEntity> findByComplianceStatusFalse();
    
    /**
     * Find quality reports for a bank with compliance issues.
     */
    List<QualityReportEntity> findByBankIdAndComplianceStatusFalse(String bankId);
    
    /**
     * Find quality reports ordered by creation time (most recent first).
     */
    List<QualityReportEntity> findAllByOrderByCreatedAtDesc();
    
    /**
     * Find quality reports for a bank ordered by creation time (most recent first).
     */
    List<QualityReportEntity> findByBankIdOrderByCreatedAtDesc(String bankId);
    
    /**
     * Find quality reports with processing duration exceeding threshold.
     */
    @Query("SELECT qr FROM QualityReportEntity qr WHERE " +
           "qr.processingDurationMs > :thresholdMs")
    List<QualityReportEntity> findSlowProcessingReports(@Param("thresholdMs") Long thresholdMs);
    
    /**
     * Find quality reports by quality grade.
     */
    @Query("SELECT qr FROM QualityReportEntity qr WHERE qr.qualityGrade = :grade")
    List<QualityReportEntity> findByQualityGrade(@Param("grade") String grade);
    
    /**
     * Get average overall score for a bank.
     */
    @Query("SELECT AVG(qr.overallScore) FROM QualityReportEntity qr WHERE " +
           "qr.bankId = :bankId AND qr.status = 'COMPLETED'")
    Optional<BigDecimal> getAverageOverallScoreByBankId(@Param("bankId") String bankId);
    
    /**
     * Get average overall score across all banks.
     */
    @Query("SELECT AVG(qr.overallScore) FROM QualityReportEntity qr WHERE qr.status = 'COMPLETED'")
    Optional<BigDecimal> getAverageOverallScore();
    
    /**
     * Count reports by compliance status.
     */
    long countByComplianceStatus(Boolean complianceStatus);
    
    /**
     * Count reports by bank and compliance status.
     */
    long countByBankIdAndComplianceStatus(String bankId, Boolean complianceStatus);
}