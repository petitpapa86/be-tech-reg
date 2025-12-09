package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity for persisting QualityErrorSummary value objects.
 * Maps to the quality_error_summaries table in the database.
 */
@Setter
@Getter
@Entity
@Table(name = "quality_error_summaries", schema = "dataquality", indexes = {
    @Index(name = "idx_error_summaries_batch_id", columnList = "batch_id"),
    @Index(name = "idx_error_summaries_batch_dimension", columnList = "batch_id, dimension"),
    @Index(name = "idx_error_summaries_batch_severity", columnList = "batch_id, severity"),
    @Index(name = "idx_error_summaries_rule_code", columnList = "rule_code"),
    @Index(name = "idx_error_summaries_dimension", columnList = "dimension"),
    @Index(name = "idx_error_summaries_severity", columnList = "severity"),
    @Index(name = "idx_error_summaries_bank_id", columnList = "bank_id"),
    @Index(name = "idx_error_summaries_bank_dimension", columnList = "bank_id, dimension"),
    @Index(name = "idx_error_summaries_created_at", columnList = "created_at"),
    @Index(name = "idx_error_summaries_error_count", columnList = "error_count")
})
public class QualityErrorSummaryEntity {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "batch_id", nullable = false, length = 255)
    private String batchId;
    
    @Column(name = "bank_id", nullable = false, length = 255)
    private String bankId;
    
    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", nullable = false, length = 20)
    private QualityDimension dimension;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ValidationError.ErrorSeverity severity;
    
    @Column(name = "error_message", nullable = false, length = 1000)
    private String errorMessage;
    
    @Column(name = "field_name", length = 100)
    private String fieldName;
    
    @Column(name = "error_count", nullable = false)
    private Integer errorCount;
    
    /**
     * Store affected exposure IDs as JSON array.
     * Limited to 10 examples to prevent database bloat.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_exposure_ids", columnDefinition = "json")
    private List<String> affectedExposureIds;
    
    // Audit Fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
    
    // Constructors
    protected QualityErrorSummaryEntity() {
        // JPA constructor
    }
    
    public QualityErrorSummaryEntity(
        String batchId,
        String bankId,
        String ruleCode,
        QualityDimension dimension,
        ValidationError.ErrorSeverity severity,
        String errorMessage,
        String fieldName,
        Integer errorCount,
        List<String> affectedExposureIds
    ) {
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.bankId = Objects.requireNonNull(bankId, "Bank ID cannot be null");
        this.ruleCode = Objects.requireNonNull(ruleCode, "Rule code cannot be null");
        this.dimension = Objects.requireNonNull(dimension, "Dimension cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");
        this.errorMessage = Objects.requireNonNull(errorMessage, "Error message cannot be null");
        this.fieldName = fieldName;
        this.errorCount = Objects.requireNonNull(errorCount, "Error count cannot be null");
        this.affectedExposureIds = Objects.requireNonNull(affectedExposureIds, "Affected exposure IDs cannot be null");
        
        if (errorCount < 0) {
            throw new IllegalArgumentException("Error count cannot be negative");
        }
        if (affectedExposureIds.size() > 10) {
            throw new IllegalArgumentException("Affected exposure IDs list cannot exceed 10 examples");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualityErrorSummaryEntity that = (QualityErrorSummaryEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "QualityErrorSummaryEntity{" +
                "id=" + id +
                ", batchId='" + batchId + '\'' +
                ", bankId='" + bankId + '\'' +
                ", ruleCode='" + ruleCode + '\'' +
                ", dimension=" + dimension +
                ", severity=" + severity +
                ", errorCount=" + errorCount +
                ", createdAt=" + createdAt +
                '}';
    }
}

