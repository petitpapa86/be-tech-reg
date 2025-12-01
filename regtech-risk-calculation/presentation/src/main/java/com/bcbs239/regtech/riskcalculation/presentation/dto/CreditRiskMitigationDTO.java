package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO for credit risk mitigation
 * Represents collateral, guarantees, and other risk mitigation instruments
 * Requirements: 1.1
 */
public class CreditRiskMitigationDTO {
    
    @JsonProperty("exposure_id")
    private String exposureId;
    
    @JsonProperty("mitigation_type")
    private String mitigationType;
    
    @JsonProperty("value")
    private BigDecimal value;
    
    @JsonProperty("currency")
    private String currency;
    
    // Default constructor for Jackson
    public CreditRiskMitigationDTO() {
    }
    
    public CreditRiskMitigationDTO(
        String exposureId,
        String mitigationType,
        BigDecimal value,
        String currency
    ) {
        this.exposureId = exposureId;
        this.mitigationType = mitigationType;
        this.value = value;
        this.currency = currency;
    }
    
    public String getExposureId() {
        return exposureId;
    }
    
    public void setExposureId(String exposureId) {
        this.exposureId = exposureId;
    }
    
    public String getMitigationType() {
        return mitigationType;
    }
    
    public void setMitigationType(String mitigationType) {
        this.mitigationType = mitigationType;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public void setValue(BigDecimal value) {
        this.value = value;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
