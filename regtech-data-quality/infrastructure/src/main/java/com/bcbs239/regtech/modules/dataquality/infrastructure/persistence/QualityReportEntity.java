package com.bcbs239.regtech.modules.dataquality.infrastructure.persistence;

import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityGrade;
import com.bcbs239.regtech.modules.dataquality.domain.report.QualityStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for persisting QualityReport aggregate data.
 * Maps to the quality_reports table in the database.
 */
@Entity
@Table(name = "quality_reports", indexes = {
    @Index(name = "idx_quality_reports_batch_id", columnList = "batch_id", unique = true),
    @Index(name = "idx_quality_reports_bank_id", columnList = "bank_id"),
    @Index(name = "idx_quality_reports_status", columnList = "status"),
    @Index(name = "idx_quality_reports_bank_status", columnList = "bank_id, status"),
    @Index(name = "idx_quality_reports_created_at", columnList = "created_at"),
    @Index(name = "idx_quality_reports_overall_score", columnList = "overall_score"),
    @Index(name = "idx_quality_reports_compliance_status", columnList = "compliance_status")
})
public class QualityReportEntity {
    
    @Id
    @Column(name = "report_id", nullable = false, length = 36)
    private String reportId;
    
    @Column(name = "batch_id", nullable = false, length = 36, unique = true)
    private String batchId;
    
    @Column(name = "bank_id", nullable = false, length = 36)
    private String bankId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QualityStatus status;
    
    // Quality Scores
    @Column(name = "completeness_score", precision = 5, scale = 2)
    private BigDecimal completenessScore;
    
    @Column(name = "accuracy_score", precision = 5, scale = 2)
    private BigDecimal accuracyScore;
    
    @Column(name = "consistency_score", precision = 5, scale = 2)
    private BigDecimal consistencyScore;
    
    @Column(name = "timeliness_score", precision = 5, scale = 2)
    private BigDecimal timelinessScore;
    
    @Column(name = "uniqueness_score", precision = 5, scale = 2)
    private BigDecimal uniquenessScore;
    
    @Column(name = "validity_score", precision = 5, scale = 2)
    private BigDecimal validityScore;
    
    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "quality_grade", length = 2)
    private QualityGrade qualityGrade;
    
    // Validation Summary
    @Column(name = "total_exposures")
    private Integer totalExposures;
    
    @Column(name = "valid_exposures")
    private Integer validExposures;
    
    @Column(name = "total_errors")
    private Integer totalErrors;
    
    @Column(name = "completeness_errors")
    private Integer completenessErrors;
    
    @Column(name = "accuracy_errors")
    private Integer accuracyErrors;
    
    @Column(name = "consistency_errors")
    private Integer consistencyErrors;
    
    @Column(name = "timeliness_errors")
    private Integer timelinessErrors;
    
    @Column(name = "uniqueness_errors")
    private Integer uniquenessErrors;
    
    @Column(name = "validity_errors")
    private Integer validityErrors;
    
    // S3 Reference
    @Column(name = "s3_bucket", length = 255)
    private String s3Bucket;
    
    @Column(name = "s3_key", length = 1024)
    private String s3Key;
    
    @Column(name = "s3_uri", length = 1024)
    private String s3Uri;
    
    // Compliance Status
    @Column(name = "compliance_status", nullable = false)
    private Boolean complianceStatus = false;
    
    // Error Information
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    // Processing Metadata
    @Column(name = "processing_start_time")
    private Instant processingStartTime;
    
    @Column(name = "processing_end_time")
    private Instant processingEndTime;
    
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;
    
    // Audit Fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    // Constructors
    protected QualityReportEntity() {
        // JPA constructor
    }
    
    public QualityReportEntity(String reportId, String batchId, String bankId, QualityStatus status) {
        this.reportId = Objects.requireNonNull(reportId, "Report ID cannot be null");
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.bankId = Objects.requireNonNull(bankId, "Bank ID cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }
    
    // Getters and Setters
    public String getReportId() {
        return reportId;
    }
    
    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
    
    public String getBatchId() {
        return batchId;
    }
    
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
    
    public String getBankId() {
        return bankId;
    }
    
    public void setBankId(String bankId) {
        this.bankId = bankId;
    }
    
    public QualityStatus getStatus() {
        return status;
    }
    
    public void setStatus(QualityStatus status) {
        this.status = status;
    }
    
    public BigDecimal getCompletenessScore() {
        return completenessScore;
    }
    
    public void setCompletenessScore(BigDecimal completenessScore) {
        this.completenessScore = completenessScore;
    }
    
    public BigDecimal getAccuracyScore() {
        return accuracyScore;
    }
    
    public void setAccuracyScore(BigDecimal accuracyScore) {
        this.accuracyScore = accuracyScore;
    }
    
    public BigDecimal getConsistencyScore() {
        return consistencyScore;
    }
    
    public void setConsistencyScore(BigDecimal consistencyScore) {
        this.consistencyScore = consistencyScore;
    }
    
    public BigDecimal getTimelinessScore() {
        return timelinessScore;
    }
    
    public void setTimelinessScore(BigDecimal timelinessScore) {
        this.timelinessScore = timelinessScore;
    }
    
    public BigDecimal getUniquenessScore() {
        return uniquenessScore;
    }
    
    public void setUniquenessScore(BigDecimal uniquenessScore) {
        this.uniquenessScore = uniquenessScore;
    }
    
    public BigDecimal getValidityScore() {
        return validityScore;
    }
    
    public void setValidityScore(BigDecimal validityScore) {
        this.validityScore = validityScore;
    }
    
    public BigDecimal getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }
    
    public QualityGrade getQualityGrade() {
        return qualityGrade;
    }
    
    public void setQualityGrade(QualityGrade qualityGrade) {
        this.qualityGrade = qualityGrade;
    }
    
    public Integer getTotalExposures() {
        return totalExposures;
    }
    
    public void setTotalExposures(Integer totalExposures) {
        this.totalExposures = totalExposures;
    }
    
    public Integer getValidExposures() {
        return validExposures;
    }
    
    public void setValidExposures(Integer validExposures) {
        this.validExposures = validExposures;
    }
    
    public Integer getTotalErrors() {
        return totalErrors;
    }
    
    public void setTotalErrors(Integer totalErrors) {
        this.totalErrors = totalErrors;
    }
    
    public Integer getCompletenessErrors() {
        return completenessErrors;
    }
    
    public void setCompletenessErrors(Integer completenessErrors) {
        this.completenessErrors = completenessErrors;
    }
    
    public Integer getAccuracyErrors() {
        return accuracyErrors;
    }
    
    public void setAccuracyErrors(Integer accuracyErrors) {
        this.accuracyErrors = accuracyErrors;
    }
    
    public Integer getConsistencyErrors() {
        return consistencyErrors;
    }
    
    public void setConsistencyErrors(Integer consistencyErrors) {
        this.consistencyErrors = consistencyErrors;
    }
    
    public Integer getTimelinessErrors() {
        return timelinessErrors;
    }
    
    public void setTimelinessErrors(Integer timelinessErrors) {
        this.timelinessErrors = timelinessErrors;
    }
    
    public Integer getUniquenessErrors() {
        return uniquenessErrors;
    }
    
    public void setUniquenessErrors(Integer uniquenessErrors) {
        this.uniquenessErrors = uniquenessErrors;
    }
    
    public Integer getValidityErrors() {
        return validityErrors;
    }
    
    public void setValidityErrors(Integer validityErrors) {
        this.validityErrors = validityErrors;
    }
    
    public String getS3Bucket() {
        return s3Bucket;
    }
    
    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }
    
    public String getS3Key() {
        return s3Key;
    }
    
    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }
    
    public String getS3Uri() {
        return s3Uri;
    }
    
    public void setS3Uri(String s3Uri) {
        this.s3Uri = s3Uri;
    }
    
    public Boolean getComplianceStatus() {
        return complianceStatus;
    }
    
    public void setComplianceStatus(Boolean complianceStatus) {
        this.complianceStatus = complianceStatus;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Instant getProcessingStartTime() {
        return processingStartTime;
    }
    
    public void setProcessingStartTime(Instant processingStartTime) {
        this.processingStartTime = processingStartTime;
    }
    
    public Instant getProcessingEndTime() {
        return processingEndTime;
    }
    
    public void setProcessingEndTime(Instant processingEndTime) {
        this.processingEndTime = processingEndTime;
    }
    
    public Long getProcessingDurationMs() {
        return processingDurationMs;
    }
    
    public void setProcessingDurationMs(Long processingDurationMs) {
        this.processingDurationMs = processingDurationMs;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityReportEntity that = (QualityReportEntity) o;
        return Objects.equals(reportId, that.reportId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(reportId);
    }
    
    @Override
    public String toString() {
        return "QualityReportEntity{" +
                "reportId='" + reportId + '\'' +
                ", batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", status=" + status +
                ", overallScore=" + overallScore +
                ", qualityGrade=" + qualityGrade +
                ", complianceStatus=" + complianceStatus +
                ", createdAt=" + createdAt +
                '}';
    }
}