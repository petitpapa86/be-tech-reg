package com.bcbs239.regtech.riskcalculation.application.integration.events;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Integration event published when risk calculation completes successfully.
 * This event notifies downstream modules (billing, reporting) that risk metrics
 * are available for a batch. Detailed results are stored in S3/filesystem and
 * can be accessed via the resultFileUri.
 * 
 * Event versioning: v1.0 - Simplified version with essential data only
 */
@Getter
public class BatchCalculationCompletedIntegrationEvent extends IntegrationEvent {
    
    private static final String EVENT_VERSION = "1.0";
    
    private final String batchId;
    private final String bankId;
    private final String resultFileUri;
    private final int totalExposures;
    private final BigDecimal totalAmountEur;
    private final Instant completedAt;
    private final String eventVersion;
    
    // Key risk indicators for billing and basic reporting
    private final BigDecimal herfindahlGeographic;
    private final BigDecimal herfindahlSector;
    
    public BatchCalculationCompletedIntegrationEvent(
            String batchId,
            String bankId,
            String resultFileUri,
            int totalExposures,
            BigDecimal totalAmountEur,
            Instant completedAt,
            BigDecimal herfindahlGeographic,
            BigDecimal herfindahlSector) {
        
        super(batchId, Maybe.none(), "BatchCalculationCompletedIntegrationEvent");
        
        this.batchId = batchId;
        this.bankId = bankId;
        this.resultFileUri = resultFileUri;
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.completedAt = completedAt;
        this.eventVersion = EVENT_VERSION;
        this.herfindahlGeographic = herfindahlGeographic;
        this.herfindahlSector = herfindahlSector;
    }
    
    @Override
    public String eventType() {
        return getEventType();
    }
    
    @Override
    public String toString() {
        return String.format(
            "BatchCalculationCompletedIntegrationEvent{batchId='%s', bankId='%s', totalExposures=%d, " +
            "totalAmountEur=%s, herfindahlGeographic=%s, herfindahlSector=%s, resultFileUri='%s', " +
            "completedAt=%s, version='%s'}",
            batchId, bankId, totalExposures, totalAmountEur, 
            herfindahlGeographic, herfindahlSector, resultFileUri, completedAt, eventVersion
        );
    }
}