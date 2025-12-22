package com.bcbs239.regtech.core.domain.events.integration;


import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class RiskCalculationCompletedInboundEvent extends DomainEvent {
    private static final String EVENT_VERSION = "1.0";

    private final String batchId;
    private final String bankId;
    private final String resultFileUri;
    private final int totalExposures;
    private final BigDecimal totalAmountEur;
    private final Instant completedAt;
    private final String eventVersion;

    @JsonCreator
    public RiskCalculationCompletedInboundEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("resultFileUri") String resultFileUri,
            @JsonProperty("totalExposures") int totalExposures,
            @JsonProperty("totalAmountEur") BigDecimal totalAmountEur,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("correlationId") String correlationId
    ) {
        super(correlationId);
        this.batchId = batchId;
        this.bankId = bankId;
        this.resultFileUri = resultFileUri;
        this.totalExposures = totalExposures;
        this.totalAmountEur = totalAmountEur;
        this.completedAt = completedAt;
        this.eventVersion = EVENT_VERSION;
    }


    public boolean isValid() {
        return batchId != null && !batchId.isEmpty()
                && bankId != null && !bankId.isEmpty()
                && resultFileUri != null && !resultFileUri.isEmpty()
                && totalExposures > 0
                && totalAmountEur != null
                && completedAt != null;
    }
}