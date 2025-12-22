package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

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
@Setter
public class RiskCalculationCompletedIntegrationEvent extends IntegrationEvent {
    
    private static final String EVENT_VERSION = "1.0";
    
    private final String batchId;
    private final String bankId;
    private final String resultFileUri;
    private final Instant completedAt;
    private int totalExposures;
    private BigDecimal totalAmountEur;
    private final String eventVersion;

    @JsonCreator
    public RiskCalculationCompletedIntegrationEvent(
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("resultFileUri") String resultFileUri,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("totalExposures") int totalExposures,
            @JsonProperty("totalAmountEur") BigDecimal totalAmountEur){
        
        super(correlationId);
        this.batchId = batchId;
        this.bankId = bankId;
        this.resultFileUri = resultFileUri;
        this.completedAt = completedAt;
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.eventVersion = EVENT_VERSION;
    }
}