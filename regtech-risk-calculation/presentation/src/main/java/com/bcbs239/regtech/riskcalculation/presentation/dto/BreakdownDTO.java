package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for geographic and sector breakdowns.
 * Aligned with Breakdown domain value object.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakdownDTO {
    
    @JsonProperty("type")
    private String type; // "GEOGRAPHIC" or "SECTOR"
    
    @JsonProperty("shares")
    private Map<String, ShareDTO> shares;
    
    /**
     * Nested DTO for individual share information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareDTO {
        
        @JsonProperty("amount_eur")
        private BigDecimal amountEur;
        
        @JsonProperty("percentage")
        private BigDecimal percentage;
    }
}
