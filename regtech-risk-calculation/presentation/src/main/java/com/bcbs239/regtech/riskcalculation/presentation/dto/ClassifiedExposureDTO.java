package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for classified exposure with exposure details, EUR amounts, and economic sector classification.
 * Aligned with ClassifiedExposure domain value object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassifiedExposureDTO {
    
    @JsonProperty("exposure_id")
    private String exposureId;
    
    @JsonProperty("net_exposure_eur")
    private BigDecimal netExposureEur;
    
    @JsonProperty("geographic_region")
    private String geographicRegion;
    
    @JsonProperty("economic_sector")
    private String economicSector;
    
    @JsonProperty("classification")
    private ExposureClassificationDTO classification;
}
