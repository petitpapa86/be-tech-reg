package com.bcbs239.regtech.reportgeneration.application.coordination;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Simple data holder for calculation completion event data.
 * This is a module-internal DTO that decouples the coordination logic
 * from external module event structures.
 * 
 * Event listeners in the presentation layer will map external events to this DTO.
 */
@Getter
public class CalculationEventData {
    
    private final String batchId;
    private final String bankId;
    private final String resultFileUri;
    private final int totalExposures;
    private final BigDecimal totalAmountEur;
    private final Instant completedAt;
    
    public CalculationEventData(
            String batchId,
            String bankId,
            String resultFileUri,
            int totalExposures,
            BigDecimal totalAmountEur,
            Instant completedAt) {
        
        this.batchId = batchId;
        this.bankId = bankId;
        this.resultFileUri = resultFileUri;
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.completedAt = completedAt;
    }
    
    @Override
    public String toString() {
        return String.format(
            "CalculationEventData{batchId='%s', bankId='%s', totalExposures=%d, " +
            "totalAmountEur=%s, resultFileUri='%s', completedAt=%s}",
            batchId, bankId, totalExposures, totalAmountEur, resultFileUri, completedAt
        );
    }
}
