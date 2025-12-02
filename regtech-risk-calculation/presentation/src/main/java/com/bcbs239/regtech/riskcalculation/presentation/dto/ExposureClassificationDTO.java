package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for classification metadata of an exposure.
 * Aligned with ExposureClassification domain value object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExposureClassificationDTO {
    
    @JsonProperty("product_type")
    private String productType;
    
    @JsonProperty("instrument_type")
    private String instrumentType;
    
    @JsonProperty("balance_sheet_type")
    private String balanceSheetType;
    
    @JsonProperty("country_code")
    private String countryCode;
}
