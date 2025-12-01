package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for risk report containing bank info, exposures, and mitigations
 * Top-level DTO for JSON risk report ingestion
 * Requirements: 1.1, 1.2
 */
public class RiskReportDTO {
    
    @JsonProperty("bank_info")
    private BankInfoDTO bankInfo;
    
    @JsonProperty("exposures")
    private List<ExposureDTO> exposures;
    
    @JsonProperty("credit_risk_mitigation")
    private List<CreditRiskMitigationDTO> creditRiskMitigation;
    
    // Default constructor for Jackson
    public RiskReportDTO() {
    }
    
    public RiskReportDTO(
        BankInfoDTO bankInfo,
        List<ExposureDTO> exposures,
        List<CreditRiskMitigationDTO> creditRiskMitigation
    ) {
        this.bankInfo = bankInfo;
        this.exposures = exposures;
        this.creditRiskMitigation = creditRiskMitigation;
    }
    
    public BankInfoDTO getBankInfo() {
        return bankInfo;
    }
    
    public void setBankInfo(BankInfoDTO bankInfo) {
        this.bankInfo = bankInfo;
    }
    
    public List<ExposureDTO> getExposures() {
        return exposures;
    }
    
    public void setExposures(List<ExposureDTO> exposures) {
        this.exposures = exposures;
    }
    
    public List<CreditRiskMitigationDTO> getCreditRiskMitigation() {
        return creditRiskMitigation;
    }
    
    public void setCreditRiskMitigation(List<CreditRiskMitigationDTO> creditRiskMitigation) {
        this.creditRiskMitigation = creditRiskMitigation;
    }
}
