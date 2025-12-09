package com.bcbs239.regtech.reportgeneration.application.integration.events;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Integration event DTO for batch calculation completion.
 * 
 * <p>This is a self-contained DTO used for cross-module communication between
 * the risk-calculation module and the report-generation module. It contains
 * only primitive types and standard library classes to avoid coupling to
 * domain objects from other modules.
 * 
 * <p>This event is received from the risk-calculation module and contains
 * essential information needed to generate comprehensive reports.
 * 
 * <p>Requirements: 2.5, 3.3
 */
@Getter
public class BatchCalculationCompletedIntegrationEvent {
    
    private final String batchId;
    private final String bankId;
    private final String resultFileUri;
    private final int totalExposures;
    private final BigDecimal totalAmountEur;
    private final Instant completedAt;
    private final BigDecimal herfindahlGeographic;
    private final BigDecimal herfindahlSector;
    private final String eventVersion;
    
    public BatchCalculationCompletedIntegrationEvent(
            String batchId,
            String bankId,
            String resultFileUri,
            int totalExposures,
            BigDecimal totalAmountEur,
            Instant completedAt,
            BigDecimal herfindahlGeographic,
            BigDecimal herfindahlSector,
            String eventVersion) {
        
        this.batchId = batchId;
        this.bankId = bankId;
        this.resultFileUri = resultFileUri;
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.completedAt = completedAt;
        this.herfindahlGeographic = herfindahlGeographic;
        this.herfindahlSector = herfindahlSector;
        this.eventVersion = eventVersion;
    }
    
    @Override
    public String toString() {
        return String.format(
            "BatchCalculationCompletedIntegrationEvent{batchId='%s', bankId='%s', " +
            "totalExposures=%d, totalAmountEur=%s, resultFileUri='%s', completedAt=%s, version='%s'}",
            batchId, bankId, totalExposures, totalAmountEur, resultFileUri, completedAt, eventVersion
        );
    }
}
