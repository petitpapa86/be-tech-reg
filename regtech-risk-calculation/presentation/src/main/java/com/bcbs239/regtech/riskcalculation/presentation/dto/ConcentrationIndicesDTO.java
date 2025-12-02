package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for concentration indices with HHI values and risk levels.
 * Aligned with HHI domain value object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcentrationIndicesDTO {
    
    @JsonProperty("geographic_hhi")
    private BigDecimal geographicHhi;
    
    @JsonProperty("geographic_risk_level")
    private String geographicRiskLevel;
    
    @JsonProperty("sector_hhi")
    private BigDecimal sectorHhi;
    
    @JsonProperty("sector_risk_level")
    private String sectorRiskLevel;
}
