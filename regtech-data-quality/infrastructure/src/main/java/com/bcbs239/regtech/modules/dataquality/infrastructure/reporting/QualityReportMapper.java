package com.bcbs239.regtech.modules.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.modules.dataquality.domain.quality.DimensionScores;
import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.modules.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.modules.dataquality.domain.report.QualityReportId;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.modules.dataquality.domain.shared.BatchId;
import com.bcbs239.regtech.modules.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ValidationSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Mapper for converting between QualityReport domain objects and QualityReportEntity JPA entities.
 */
@Component
public class QualityReportMapper {
    
    /**
     * Convert domain QualityReport to JPA entity.
     */
    public QualityReportEntity toEntity(QualityReport domainReport) {
        if (domainReport == null) {
            return null;
        }
        
        QualityReportEntity entity = new QualityReportEntity(
            domainReport.getReportId().getValue(),
            domainReport.getBatchId().getValue(),
            domainReport.getBankId().getValue(),
            domainReport.getStatus()
        );
        
        // Map quality scores
        QualityScores scores = domainReport.getScores();
        if (scores != null) {
            entity.setCompletenessScore(BigDecimal.valueOf(scores.completenessScore()));
            entity.setAccuracyScore(BigDecimal.valueOf(scores.accuracyScore()));
            entity.setConsistencyScore(BigDecimal.valueOf(scores.consistencyScore()));
            entity.setTimelinessScore(BigDecimal.valueOf(scores.timelinessScore()));
            entity.setUniquenessScore(BigDecimal.valueOf(scores.uniquenessScore()));
            entity.setValidityScore(BigDecimal.valueOf(scores.validityScore()));
            entity.setOverallScore(BigDecimal.valueOf(scores.overallScore()));
            entity.setQualityGrade(scores.grade());
        }
        
        // Map validation summary
        ValidationSummary summary = domainReport.getValidationSummary();
        if (summary != null) {
            entity.setTotalExposures(summary.totalExposures());
            entity.setValidExposures(summary.validExposures());
            entity.setTotalErrors(summary.totalErrors());
            entity.setCompletenessErrors(summary.completenessErrors());
            entity.setAccuracyErrors(summary.accuracyErrors());
            entity.setConsistencyErrors(summary.consistencyErrors());
            entity.setTimelinessErrors(summary.timelinessErrors());
            entity.setUniquenessErrors(summary.uniquenessErrors());
            entity.setValidityErrors(summary.validityErrors());
        }
        
        // Map S3 reference
        S3Reference s3Reference = domainReport.getDetailsReference();
        if (s3Reference != null) {
            entity.setS3Bucket(s3Reference.bucket());
            entity.setS3Key(s3Reference.key());
            entity.setS3Uri(s3Reference.uri());
        }
        
        // Map compliance status
        entity.setComplianceStatus(domainReport.isCompliant());
        
        // Map error message
        entity.setErrorMessage(domainReport.getErrorMessage());
        
        // Map processing metadata
        entity.setProcessingStartTime(domainReport.getProcessingStartTime());
        entity.setProcessingEndTime(domainReport.getProcessingEndTime());
        entity.setProcessingDurationMs(domainReport.getProcessingDurationMs());
        
        // Map audit fields
        entity.setCreatedAt(domainReport.getCreatedAt());
        entity.setUpdatedAt(domainReport.getUpdatedAt());
        
        return entity;
    }
    
    /**
     * Convert JPA entity to domain QualityReport.
     */
    public QualityReport toDomain(QualityReportEntity entity) {
        if (entity == null) {
            return null;
        }
        
        // Create quality scores
        QualityScores scores = null;
        if (entity.getOverallScore() != null) {
            scores = new QualityScores(
                Optional.ofNullable(entity.getCompletenessScore()).map(BigDecimal::doubleValue).orElse(0.0),
                Optional.ofNullable(entity.getAccuracyScore()).map(BigDecimal::doubleValue).orElse(0.0),
                Optional.ofNullable(entity.getConsistencyScore()).map(BigDecimal::doubleValue).orElse(0.0),
                Optional.ofNullable(entity.getTimelinessScore()).map(BigDecimal::doubleValue).orElse(0.0),
                Optional.ofNullable(entity.getUniquenessScore()).map(BigDecimal::doubleValue).orElse(0.0),
                Optional.ofNullable(entity.getValidityScore()).map(BigDecimal::doubleValue).orElse(0.0),
                entity.getOverallScore().doubleValue(),
                entity.getQualityGrade()
            );
        }
        
        // Create validation summary
        ValidationSummary summary = null;
        if (entity.getTotalExposures() != null) {
            summary = new ValidationSummary(
                Optional.ofNullable(entity.getTotalExposures()).orElse(0),
                Optional.ofNullable(entity.getValidExposures()).orElse(0),
                Optional.ofNullable(entity.getTotalErrors()).orElse(0),
                Optional.ofNullable(entity.getCompletenessErrors()).orElse(0),
                Optional.ofNullable(entity.getAccuracyErrors()).orElse(0),
                Optional.ofNullable(entity.getConsistencyErrors()).orElse(0),
                Optional.ofNullable(entity.getTimelinessErrors()).orElse(0),
                Optional.ofNullable(entity.getUniquenessErrors()).orElse(0),
                Optional.ofNullable(entity.getValidityErrors()).orElse(0)
            );
        }
        
        // Create S3 reference
        S3Reference s3Reference = null;
        if (entity.getS3Uri() != null) {
            s3Reference = new S3Reference(
                entity.getS3Bucket(),
                entity.getS3Key(),
                entity.getS3Uri()
            );
        }
        
        // Create domain object using factory method
        QualityReport domainReport = QualityReport.createForBatch(
            new BatchId(entity.getBatchId()),
            new BankId(entity.getBankId())
        );
        
        // Set the report ID (this would normally be done during creation)
        domainReport.setReportId(new QualityReportId(entity.getReportId()));
        
        // Set status
        domainReport.setStatus(entity.getStatus());
        
        // Set scores if available
        if (scores != null) {
            domainReport.setScores(scores);
        }
        
        // Set validation summary if available
        if (summary != null) {
            domainReport.setValidationSummary(summary);
        }
        
        // Set S3 reference if available
        if (s3Reference != null) {
            domainReport.setDetailsReference(s3Reference);
        }
        
        // Set error message if available
        if (entity.getErrorMessage() != null) {
            domainReport.setErrorMessage(entity.getErrorMessage());
        }
        
        // Set processing metadata
        domainReport.setProcessingStartTime(entity.getProcessingStartTime());
        domainReport.setProcessingEndTime(entity.getProcessingEndTime());
        domainReport.setProcessingDurationMs(entity.getProcessingDurationMs());
        
        // Set audit fields
        domainReport.setCreatedAt(entity.getCreatedAt());
        domainReport.setUpdatedAt(entity.getUpdatedAt());
        
        return domainReport;
    }
}