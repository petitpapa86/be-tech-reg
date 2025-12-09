package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.dataquality.domain.report.QualityErrorSummary;
import com.bcbs239.regtech.dataquality.domain.shared.BankId;
import com.bcbs239.regtech.dataquality.domain.shared.BatchId;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between QualityErrorSummary domain objects and QualityErrorSummaryEntity JPA entities.
 */
@Component
public class QualityErrorSummaryMapper {
    
    /**
     * Convert domain QualityErrorSummary to JPA entity.
     */
    public QualityErrorSummaryEntity toEntity(QualityErrorSummary domainSummary, BankId bankId) {
        if (domainSummary == null) {
            return null;
        }
        
        return new QualityErrorSummaryEntity(
            domainSummary.batchId().value(),
            bankId.value(),
            domainSummary.ruleCode(),
            domainSummary.dimension(),
            domainSummary.severity(),
            domainSummary.errorMessage(),
            domainSummary.fieldName(),
            domainSummary.errorCount(),
            domainSummary.affectedExposureIds()
        );
    }
    
    /**
     * Convert JPA entity to domain QualityErrorSummary.
     */
    public QualityErrorSummary toDomain(QualityErrorSummaryEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new QualityErrorSummary(
            new BatchId(entity.getBatchId()),
            entity.getRuleCode(),
            entity.getDimension(),
            entity.getSeverity(),
            entity.getErrorMessage(),
            entity.getFieldName(),
            entity.getErrorCount(),
            entity.getAffectedExposureIds()
        );
    }
}

