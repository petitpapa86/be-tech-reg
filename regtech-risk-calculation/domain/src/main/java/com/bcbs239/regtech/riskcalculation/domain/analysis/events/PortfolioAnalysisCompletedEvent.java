package com.bcbs239.regtech.riskcalculation.domain.analysis.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
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
    
    public PortfolioAnalysisCompletedEvent(
            String batchId,
            BigDecimal totalPortfolioEur,
            BigDecimal geographicHHI,
            BigDecimal sectorHHI,
            Instant completedAt) {
        super(batchId, "PortfolioAnalysisCompleted");
        this.batchId = batchId;
        this.totalPortfolioEur = totalPortfolioEur;
        this.geographicHHI = geographicHHI;
        this.sectorHHI = sectorHHI;
        this.completedAt = completedAt;
    }
    
    @Override
    public String eventType() {
        return "PortfolioAnalysisCompleted";
    }

}
