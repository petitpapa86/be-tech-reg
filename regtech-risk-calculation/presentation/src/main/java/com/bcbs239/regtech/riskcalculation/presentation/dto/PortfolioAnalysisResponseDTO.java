package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for complete portfolio analysis response with processing state, concentration indices, 
 * breakdowns, and timestamps.
 * Aligned with PortfolioAnalysis domain aggregate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAnalysisResponseDTO {
    
    @JsonProperty("batch_id")
    private String batchId;
    
    @JsonProperty("total_portfolio_eur")
    private BigDecimal totalPortfolioEur;
    
    @JsonProperty("processing_state")
    private ProcessingStateDTO processingState;
    
    @JsonProperty("processing_progress")
    private ProcessingProgressDTO processingProgress;
    
    @JsonProperty("concentration_indices")
    private ConcentrationIndicesDTO concentrationIndices;
    
    @JsonProperty("geographic_breakdown")
    private BreakdownDTO geographicBreakdown;
    
    @JsonProperty("sector_breakdown")
    private BreakdownDTO sectorBreakdown;
    
    @JsonProperty("timestamps")
    private ProcessingTimestampsDTO timestamps;
}
