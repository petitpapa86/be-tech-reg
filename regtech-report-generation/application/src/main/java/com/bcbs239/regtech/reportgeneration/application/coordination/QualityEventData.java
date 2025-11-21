package com.bcbs239.regtech.reportgeneration.application.coordination;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Simple data holder for quality completion event data.
 * This is a module-internal DTO that decouples the coordination logic
 * from external module event structures.
 * 
 * Event listeners in the presentation layer will map external events to this DTO.
 */
@Getter
public class QualityEventData {
    
    private final String batchId;
    private final String bankId;
    private final String resultFileUri;
    private final BigDecimal overallScore;
    private final String grade;
    private final Instant completedAt;
    
    public QualityEventData(
            String batchId,
            String bankId,
            String resultFileUri,
            BigDecimal overallScore,
            String grade,
            Instant completedAt) {
        
        this.batchId = batchId;
        this.bankId = bankId;
        this.resultFileUri = resultFileUri;
        this.overallScore = overallScore;
        this.grade = grade;
        this.completedAt = completedAt;
    }
    
    @Override
    public String toString() {
        return String.format(
            "QualityEventData{batchId='%s', bankId='%s', overallScore=%s, " +
            "grade='%s', resultFileUri='%s', completedAt=%s}",
            batchId, bankId, overallScore, grade, resultFileUri, completedAt
        );
    }
}
