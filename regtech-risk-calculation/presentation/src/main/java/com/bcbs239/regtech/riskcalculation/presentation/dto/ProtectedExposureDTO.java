package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for protected exposure with gross/net exposure and applied mitigations.
 * Aligned with ProtectedExposure domain aggregate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtectedExposureDTO {
    
    @JsonProperty("exposure_id")
    private String exposureId;
    
    @JsonProperty("gross_exposure_eur")
    private BigDecimal grossExposureEur;
    
    @JsonProperty("net_exposure_eur")
    private BigDecimal netExposureEur;
    
    @JsonProperty("total_mitigation_eur")
    private BigDecimal totalMitigationEur;
    
    @JsonProperty("mitigations")
    private List<MitigationDTO> mitigations;
    
    @JsonProperty("has_mitigations")
    private boolean hasMitigations;
    
    @JsonProperty("fully_covered")
    private boolean fullyCovered;
}
