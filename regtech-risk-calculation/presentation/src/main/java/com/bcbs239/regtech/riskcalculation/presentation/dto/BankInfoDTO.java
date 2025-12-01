package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for bank information
 * Used for JSON serialization/deserialization of bank metadata
 * Requirements: 1.1
 */
public class BankInfoDTO {
    
    @JsonProperty("bank_name")
    private String bankName;
    
    @JsonProperty("abi_code")
    private String abiCode;
    
    @JsonProperty("lei_code")
    private String leiCode;
    
    // Default constructor for Jackson
    public BankInfoDTO() {
    }
    
    public BankInfoDTO(String bankName, String abiCode, String leiCode) {
        this.bankName = bankName;
        this.abiCode = abiCode;
        this.leiCode = leiCode;
    }
    
    public String getBankName() {
        return bankName;
    }
    
    public void setBankName(String bankName) {
        this.bankName = bankName;
    }
    
    public String getAbiCode() {
        return abiCode;
    }
    
    public void setAbiCode(String abiCode) {
        this.abiCode = abiCode;
    }
    
    public String getLeiCode() {
        return leiCode;
    }
    
    public void setLeiCode(String leiCode) {
        this.leiCode = leiCode;
    }
}
