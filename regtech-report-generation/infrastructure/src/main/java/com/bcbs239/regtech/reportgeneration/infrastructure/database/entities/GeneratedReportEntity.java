package com.bcbs239.regtech.reportgeneration.infrastructure.database.entities;

import com.bcbs239.regtech.reportgeneration.domain.generation.GeneratedReport;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Entity for GeneratedReport aggregate persistence.
 * Maps domain aggregate to report_generation_summaries table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "report_generation_summaries", indexes = {
    @Index(name = "idx_report_batch_id", columnList = "batch_id"),
    @Index(name = "idx_report_bank_id", columnList = "bank_id"),
    @Index(name = "idx_report_status", columnList = "status")
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

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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

    @Column(name = "xbrl_validation_status", length = 20)
    private String xbrlValidationStatus;

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
        entity.setStatus(report.getStatus().name());
        
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
            entity.setXbrlValidationStatus(xbrlMetadata.validationStatus().name());
        }
        
        // Map timestamps
        entity.setGeneratedAt(report.getTimestamps().startedAt());
        if (report.getTimestamps().completedAt() != null) {
            entity.setCompletedAt(report.getTimestamps().completedAt());
        }
        
        // Map failure reason if present
        report.getFailureReason().ifPresent(reason -> 
            entity.setFailureReason(reason.message())
        );
        
        return entity;
    }

    /**
     * Convert JPA entity to domain aggregate
     * Uses reflection to set private fields since builder is private
     * 
     * @return the domain aggregate
     */
    public GeneratedReport toDomain() {
        try {
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
                        ? XbrlValidationStatus.valueOf(this.xbrlValidationStatus)
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
            Optional<FailureReason> failureReason = Optional.ofNullable(this.failureReason)
                    .map(reason -> FailureReason.of(FailureReason.FailureCategory.UNKNOWN, reason));
            
            // Use reflection to create instance and set fields
            // This is necessary because the builder is private
            Class<?> builderClass = Class.forName("com.bcbs239.regtech.reportgeneration.domain.generation.GeneratedReport$GeneratedReportBuilder");
            Object builder = builderClass.getDeclaredConstructor().newInstance();
            
            // Set fields using builder methods
            builderClass.getMethod("reportId", ReportId.class).invoke(builder, ReportId.of(this.reportId));
            builderClass.getMethod("batchId", BatchId.class).invoke(builder, BatchId.of(this.batchId));
            builderClass.getMethod("bankId", BankId.class).invoke(builder, BankId.of(this.bankId));
            builderClass.getMethod("reportingDate", ReportingDate.class).invoke(builder, ReportingDate.of(this.reportingDate));
            builderClass.getMethod("status", ReportStatus.class).invoke(builder, ReportStatus.valueOf(this.status));
            builderClass.getMethod("htmlMetadata", HtmlReportMetadata.class).invoke(builder, htmlMetadata);
            builderClass.getMethod("xbrlMetadata", XbrlReportMetadata.class).invoke(builder, xbrlMetadata);
            builderClass.getMethod("timestamps", ProcessingTimestamps.class).invoke(builder, timestamps);
            builderClass.getMethod("failureReason", Optional.class).invoke(builder, failureReason);
            
            // Build the aggregate
            GeneratedReport report = (GeneratedReport) builderClass.getMethod("build").invoke(builder);
            
            return report;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert entity to domain aggregate", e);
        }
    }
}
