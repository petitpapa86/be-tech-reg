package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for credit risk mitigation details.
 * Aligned with Mitigation domain entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MitigationDTO {
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("value_eur")
    private BigDecimal valueEur;
}
