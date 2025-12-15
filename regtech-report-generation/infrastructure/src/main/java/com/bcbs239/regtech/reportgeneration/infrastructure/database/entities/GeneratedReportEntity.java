package com.bcbs239.regtech.reportgeneration.infrastructure.database.entities;

import com.bcbs239.regtech.reportgeneration.domain.generation.GeneratedReport;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity for GeneratedReport aggregate persistence.
 * Maps domain aggregate to generated_reports table in reportgeneration schema.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "generated_reports", schema = "reportgeneration", indexes = {
    @Index(name = "idx_generated_reports_batch_id", columnList = "batch_id"),
    @Index(name = "idx_generated_reports_bank_id", columnList = "bank_id"),
    @Index(name = "idx_generated_reports_status", columnList = "status"),
    @Index(name = "idx_generated_reports_reporting_date", columnList = "reporting_date"),
    @Index(name = "idx_generated_reports_generated_at", columnList = "generated_at")
})
public class GeneratedReportEntity {

    @Id
    @Column(name = "report_id", nullable = false)
    private UUID reportId;

    @Column(name = "batch_id", nullable = false, unique = true, length = 255)
    private String batchId;

    @Column(name = "bank_id", nullable = false, length = 20)
    private String bankId;

    @Column(name = "reporting_date", nullable = false)
    private LocalDate reportingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "html_s3_uri", columnDefinition = "TEXT")
    private String htmlS3Uri;

    @Column(name = "html_file_size")
    private Long htmlFileSize;

    @Column(name = "html_presigned_url", columnDefinition = "TEXT")
    private String htmlPresignedUrl;

    @Column(name = "xbrl_s3_uri", columnDefinition = "TEXT")
    private String xbrlS3Uri;

    @Column(name = "xbrl_file_size")
    private Long xbrlFileSize;

    @Column(name = "xbrl_presigned_url", columnDefinition = "TEXT")
    private String xbrlPresignedUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "xbrl_validation_status", length = 50)
    private XbrlValidationStatus xbrlValidationStatus;

    @Column(name = "overall_quality_score", precision = 5, scale = 2)
    private java.math.BigDecimal overallQualityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "compliance_status", length = 50)
    private ComplianceStatus complianceStatus;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Convert domain aggregate to JPA entity
     * 
     * @param report the domain aggregate
     * @return the JPA entity
     */
    public static GeneratedReportEntity fromDomain(GeneratedReport report) {
        GeneratedReportEntity entity = new GeneratedReportEntity();
        
        entity.setReportId(report.getReportId().value());
        entity.setBatchId(report.getBatchId().value());
        entity.setBankId(report.getBankId().value());
        entity.setReportingDate(report.getReportingDate().value());
        entity.setReportType(report.getReportType());
        entity.setStatus(report.getStatus());
        
        // Map quality metrics
        entity.setOverallQualityScore(report.getOverallQualityScore());
        if (report.getComplianceStatus() != null) {
            entity.setComplianceStatus(report.getComplianceStatus());
        }
        
        // Map HTML metadata if present
        if (report.getHtmlMetadata() != null) {
            HtmlReportMetadata htmlMetadata = report.getHtmlMetadata();
            entity.setHtmlS3Uri(htmlMetadata.s3Uri().value());
            entity.setHtmlFileSize(htmlMetadata.fileSize().bytes());
            entity.setHtmlPresignedUrl(htmlMetadata.presignedUrl().url());
        }
        
        // Map XBRL metadata if present
        if (report.getXbrlMetadata() != null) {
            XbrlReportMetadata xbrlMetadata = report.getXbrlMetadata();
            entity.setXbrlS3Uri(xbrlMetadata.s3Uri().value());
            entity.setXbrlFileSize(xbrlMetadata.fileSize().bytes());
            entity.setXbrlPresignedUrl(xbrlMetadata.presignedUrl().url());
            entity.setXbrlValidationStatus(xbrlMetadata.validationStatus());
        }
        
        // Map timestamps
        entity.setGeneratedAt(report.getTimestamps().startedAt());
        if (report.getTimestamps().completedAt() != null) {
            entity.setCompletedAt(report.getTimestamps().completedAt());
        }
        
        // Map failure reason if present
        if (report.getFailureReason() != null) {
            entity.setFailureReason(report.getFailureReason().message());
        }
        
        return entity;
    }

    /**
     * Update existing JPA entity from domain aggregate
     * 
     * @param report the domain aggregate to update from
     * @return the updated entity
     */
    public GeneratedReportEntity updateFromDomain(GeneratedReport report) {
        this.setBankId(report.getBankId().value());
        this.setReportingDate(report.getReportingDate().value());
        this.setReportType(report.getReportType());
        this.setStatus(report.getStatus());
        
        // Map quality metrics
        this.setOverallQualityScore(report.getOverallQualityScore());
        if (report.getComplianceStatus() != null) {
            this.setComplianceStatus(report.getComplianceStatus());
        }
        
        // Map HTML metadata if present
        if (report.getHtmlMetadata() != null) {
            HtmlReportMetadata htmlMetadata = report.getHtmlMetadata();
            this.setHtmlS3Uri(htmlMetadata.s3Uri().value());
            this.setHtmlFileSize(htmlMetadata.fileSize().bytes());
            this.setHtmlPresignedUrl(htmlMetadata.presignedUrl().url());
        }
        
        // Map XBRL metadata if present
        if (report.getXbrlMetadata() != null) {
            XbrlReportMetadata xbrlMetadata = report.getXbrlMetadata();
            this.setXbrlS3Uri(xbrlMetadata.s3Uri().value());
            this.setXbrlFileSize(xbrlMetadata.fileSize().bytes());
            this.setXbrlPresignedUrl(xbrlMetadata.presignedUrl().url());
            this.setXbrlValidationStatus(xbrlMetadata.validationStatus());
        }
        
        // Map timestamps
        if (report.getTimestamps().completedAt() != null) {
            this.setCompletedAt(report.getTimestamps().completedAt());
        }
        
        // Map failure reason if present
        if (report.getFailureReason() != null) {
            this.setFailureReason(report.getFailureReason().message());
        }
        
        return this;
    }

    /**
     * Convert JPA entity to domain aggregate
     * Uses package-private reconstruction method to avoid reflection
     * 
     * @return the domain aggregate
     */
    public GeneratedReport toDomain() {
        // Create HTML metadata if present
        HtmlReportMetadata htmlMetadata = null;
        if (this.htmlS3Uri != null) {
            // PresignedUrl requires URL and expiration time
            // For reconstructed entities, we'll use a default expiration of 1 hour from now
            Instant presignedExpiration = Instant.now().plusSeconds(3600);
            
            htmlMetadata = new HtmlReportMetadata(
                    new S3Uri(this.htmlS3Uri),
                    FileSize.ofBytes(this.htmlFileSize),
                    new PresignedUrl(this.htmlPresignedUrl, presignedExpiration),
                    this.generatedAt
            );
        }
        
        // Create XBRL metadata if present
        XbrlReportMetadata xbrlMetadata = null;
        if (this.xbrlS3Uri != null) {
            XbrlValidationStatus validationStatus = this.xbrlValidationStatus != null
                    ? this.xbrlValidationStatus
                    : XbrlValidationStatus.NOT_VALIDATED;
            
            // PresignedUrl requires URL and expiration time
            Instant presignedExpiration = Instant.now().plusSeconds(3600);
            
            xbrlMetadata = new XbrlReportMetadata(
                    new S3Uri(this.xbrlS3Uri),
                    FileSize.ofBytes(this.xbrlFileSize),
                    new PresignedUrl(this.xbrlPresignedUrl, presignedExpiration),
                    validationStatus,
                    this.generatedAt
            );
        }
        
        // Create timestamps
        ProcessingTimestamps timestamps = this.completedAt != null
                ? new ProcessingTimestamps(this.generatedAt, null, null, this.completedAt, null)
                : ProcessingTimestamps.startedAt(this.generatedAt);
        
        // Create failure reason if present
        FailureReason failureReason = this.failureReason != null
                ? FailureReason.of(this.failureReason)
                : null;
        
        // Use package-private reconstruction method (no reflection needed)
        return GeneratedReport.reconstituteFromPersistence(
                ReportId.of(this.reportId),
                BatchId.of(this.batchId),
                BankId.of(this.bankId),
                ReportingDate.of(this.reportingDate),
                this.reportType,
                this.status,
                htmlMetadata,
                xbrlMetadata,
                this.overallQualityScore,
                this.complianceStatus,
                timestamps,
                failureReason
        );
    }
}
