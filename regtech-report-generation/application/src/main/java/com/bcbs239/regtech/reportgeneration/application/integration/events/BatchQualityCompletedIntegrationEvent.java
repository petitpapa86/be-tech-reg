package com.bcbs239.regtech.reportgeneration.application.integration.events;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Integration event DTO for batch quality validation completion.
 * 
 * <p>This is a self-contained DTO used for cross-module communication between
 * the data-quality module and the report-generation module. It contains
 * only primitive types and standard library classes to avoid coupling to
 * domain objects from other modules.
 * 
 * <p>This event is received from the data-quality module and contains
 * essential quality metrics needed to generate comprehensive reports.
 * 
 * <p>Requirements: 2.5, 3.3
 */
@Getter
public class BatchQualityCompletedIntegrationEvent {
    
    private final String batchId;
    private final String bankId;
    private final String s3ReferenceUri;
    private final BigDecimal overallScore;
    private final String qualityGrade;
    private final Map<String, Object> validationSummary;
    private final Map<String, Object> processingMetadata;
    private final Instant timestamp;
    
    public BatchQualityCompletedIntegrationEvent(
            String batchId,
            String bankId,
            String s3ReferenceUri,
            BigDecimal overallScore,
            String qualityGrade,
            Map<String, Object> validationSummary,
            Map<String, Object> processingMetadata,
            Instant timestamp) {
        
        this.batchId = batchId;
        this.bankId = bankId;
        this.s3ReferenceUri = s3ReferenceUri;
        this.overallScore = overallScore;
        this.qualityGrade = qualityGrade;
        this.validationSummary = validationSummary;
        this.processingMetadata = processingMetadata;
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return String.format(
            "BatchQualityCompletedIntegrationEvent{batchId='%s', bankId='%s', " +
            "overallScore=%s, grade='%s', s3ReferenceUri='%s', timestamp=%s}",
            batchId, bankId, overallScore, qualityGrade, s3ReferenceUri, timestamp
        );
    }
}
