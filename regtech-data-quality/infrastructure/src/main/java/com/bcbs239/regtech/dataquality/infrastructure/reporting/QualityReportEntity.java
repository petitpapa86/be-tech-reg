package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.dataquality.domain.quality.QualityGrade;
import com.bcbs239.regtech.dataquality.domain.report.QualityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for persisting QualityReport aggregate data.
 * Maps to the quality_reports table in the database.
 */
@Setter
@Getter
@Entity
@Table(name = "quality_reports", schema = "dataquality", indexes = {
    @Index(name = "idx_quality_reports_batch_id", columnList = "batch_id", unique = true),
    @Index(name = "idx_quality_reports_bank_id", columnList = "bank_id"),
    @Index(name = "idx_quality_reports_status", columnList = "status"),
    @Index(name = "idx_quality_reports_bank_status", columnList = "bank_id, status"),
    @Index(name = "idx_quality_reports_created_at", columnList = "created_at"),
    @Index(name = "idx_quality_reports_overall_score", columnList = "overall_score"),
    @Index(name = "idx_quality_reports_compliance_status", columnList = "compliance_status")
})
public class QualityReportEntity {

    // Getters and Setters
    @Id
    @Column(name = "report_id", nullable = false, length = 255)
    private String reportId;

    @Column(name = "batch_id", nullable = false, length = 255, unique = true)
    private String batchId;

    @Column(name = "bank_id", nullable = false, length = 255)
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
    @Column(name = "quality_grade", length = 20)
    private QualityGrade qualityGrade;

    @Column(name = "filename")
    private String filename;

    @Column(name = "file_format", length = 50)
    private String fileFormat;

    @Column(name = "file_size")
    private Long fileSize;
    
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

