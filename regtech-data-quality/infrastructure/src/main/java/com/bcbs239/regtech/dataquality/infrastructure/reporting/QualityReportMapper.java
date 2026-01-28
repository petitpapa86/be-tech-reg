package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.dataquality.domain.quality.QualityDimension;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.report.FileMetadata;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.core.domain.shared.valueobjects.QualityReportId;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.dataquality.domain.shared.S3Reference;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationSummary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
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
            domainReport.getReportId().value(),
            domainReport.getBatchId().value(),
            domainReport.getBankId().value(),
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

            // errorsByDimension map -> individual columns
            Map<QualityDimension, Integer> byDim = summary.errorsByDimension();
            if (byDim != null) {
                entity.setCompletenessErrors(byDim.getOrDefault(QualityDimension.COMPLETENESS, 0));
                entity.setAccuracyErrors(byDim.getOrDefault(QualityDimension.ACCURACY, 0));
                entity.setConsistencyErrors(byDim.getOrDefault(QualityDimension.CONSISTENCY, 0));
                entity.setTimelinessErrors(byDim.getOrDefault(QualityDimension.TIMELINESS, 0));
                entity.setUniquenessErrors(byDim.getOrDefault(QualityDimension.UNIQUENESS, 0));
                entity.setValidityErrors(byDim.getOrDefault(QualityDimension.VALIDITY, 0));
            }
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

        // Map file metadata
        if (domainReport.getFileMetadata() != null) {
            entity.setFilename(domainReport.getFileMetadata().filename());
            entity.setFileFormat(domainReport.getFileMetadata().format());
            entity.setFileSize(domainReport.getFileMetadata().size());
        }
        
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
            Map<QualityDimension, Integer> errorsByDimension = new HashMap<>();
            errorsByDimension.put(QualityDimension.COMPLETENESS, Optional.ofNullable(entity.getCompletenessErrors()).orElse(0));
            errorsByDimension.put(QualityDimension.ACCURACY, Optional.ofNullable(entity.getAccuracyErrors()).orElse(0));
            errorsByDimension.put(QualityDimension.CONSISTENCY, Optional.ofNullable(entity.getConsistencyErrors()).orElse(0));
            errorsByDimension.put(QualityDimension.TIMELINESS, Optional.ofNullable(entity.getTimelinessErrors()).orElse(0));
            errorsByDimension.put(QualityDimension.UNIQUENESS, Optional.ofNullable(entity.getUniquenessErrors()).orElse(0));
            errorsByDimension.put(QualityDimension.VALIDITY, Optional.ofNullable(entity.getValidityErrors()).orElse(0));

            double overallRate = 0.0;
            if (entity.getTotalExposures() != null && entity.getTotalExposures() > 0) {
                overallRate = (double) Optional.ofNullable(entity.getValidExposures()).orElse(0) / entity.getTotalExposures();
            }

            summary = ValidationSummary.builder()
                .totalExposures(Optional.ofNullable(entity.getTotalExposures()).orElse(0))
                .validExposures(Optional.ofNullable(entity.getValidExposures()).orElse(0))
                .totalErrors(Optional.ofNullable(entity.getTotalErrors()).orElse(0))
                .errorsByDimension(errorsByDimension)
                .errorsBySeverity(Map.of())
                .errorsByCode(Map.of())
                .overallValidationRate(overallRate)
                .build();
        }
        
        // Create S3 reference
        S3Reference s3Reference = null;
        if (entity.getS3Bucket() != null && entity.getS3Key() != null) {
            s3Reference = S3Reference.of(entity.getS3Bucket(), entity.getS3Key(), "0");
        } else if (entity.getS3Uri() != null && !entity.getS3Uri().isBlank() && entity.getS3Uri().startsWith("s3://")) {
            // Backward/compat: some rows may only store the full URI.
            // Format: s3://<bucket>/<key>
            String withoutScheme = entity.getS3Uri().substring("s3://".length());
            int firstSlash = withoutScheme.indexOf('/');
            if (firstSlash > 0 && firstSlash < withoutScheme.length() - 1) {
                String bucket = withoutScheme.substring(0, firstSlash);
                String key = withoutScheme.substring(firstSlash + 1);
                if (!bucket.isBlank() && !key.isBlank()) {
                    s3Reference = S3Reference.of(bucket, key, "0");
                }
            }
        }
        
        // Create domain object using factory method
        QualityReport domainReport = QualityReport.createForBatch(
            new BatchId(entity.getBatchId()),
            new BankId(entity.getBankId())
        );
        
        // Set the report ID (this would normally be done during creation)
        domainReport.setReportId(QualityReportId.of(entity.getReportId()));

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

        // Set file metadata
        if (entity.getFilename() != null && entity.getFileFormat() != null) {
            domainReport.setFileMetadata(FileMetadata.of(
                entity.getFilename(),
                entity.getFileFormat(),
                entity.getFileSize() != null ? entity.getFileSize() : 0L
            ));
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


