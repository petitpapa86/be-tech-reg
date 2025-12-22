package com.bcbs239.regtech.riskcalculation.domain.analysis.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event raised when portfolio analysis is completed.
 * This event captures the key metrics calculated during analysis.
 */
@Getter
public class PortfolioAnalysisCompletedEvent extends DomainEvent {
    
    private final String batchId;
    private final BigDecimal totalPortfolioEur;
    private final BigDecimal geographicHHI;
    private final BigDecimal sectorHHI;
    private final Instant completedAt;
    
    @JsonCreator
    public PortfolioAnalysisCompletedEvent(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("totalPortfolioEur") BigDecimal totalPortfolioEur,
            @JsonProperty("geographicHHI") BigDecimal geographicHHI,
            @JsonProperty("sectorHHI") BigDecimal sectorHHI,
            @JsonProperty("completedAt") Instant completedAt,
            @JsonProperty("correlationId") String correlationId) {
        super(correlationId);
        this.batchId = batchId;
        this.totalPortfolioEur = totalPortfolioEur;
        this.geographicHHI = geographicHHI;
        this.sectorHHI = sectorHHI;
        this.completedAt = completedAt;
    }


}
