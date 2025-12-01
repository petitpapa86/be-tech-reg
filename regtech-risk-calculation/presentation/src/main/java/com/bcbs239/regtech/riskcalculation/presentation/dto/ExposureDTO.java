package com.bcbs239.regtech.riskcalculation.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * DTO for generic financial exposure
 * Supports any type of financial instrument (LOAN, BOND, DERIVATIVE, GUARANTEE, etc.)
 * Requirements: 1.1, 1.2
 */
public class ExposureDTO {
    
    @JsonProperty("exposure_id")
    private String exposureId;
    
    @JsonProperty("instrument_id")
    private String instrumentId;
    
    @JsonProperty("instrument_type")
    private String instrumentType;
    
    @JsonProperty("counterparty_name")
    private String counterpartyName;
    
    @JsonProperty("counterparty_id")
    private String counterpartyId;
    
    @JsonProperty("counterparty_lei")
    private String counterpartyLei;
    
    @JsonProperty("exposure_amount")
    private BigDecimal exposureAmount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("product_type")
    private String productType;
    
    @JsonProperty("balance_sheet_type")
    private String balanceSheetType;
    
    @JsonProperty("country_code")
    private String countryCode;
    
    // Default constructor for Jackson
    public ExposureDTO() {
    }
    
    public ExposureDTO(
        String exposureId,
        String instrumentId,
        String instrumentType,
        String counterpartyName,
        String counterpartyId,
        String counterpartyLei,
        BigDecimal exposureAmount,
        String currency,
        String productType,
        String balanceSheetType,
        String countryCode
    ) {
        this.exposureId = exposureId;
        this.instrumentId = instrumentId;
        this.instrumentType = instrumentType;
        this.counterpartyName = counterpartyName;
        this.counterpartyId = counterpartyId;
        this.counterpartyLei = counterpartyLei;
        this.exposureAmount = exposureAmount;
        this.currency = currency;
        this.productType = productType;
        this.balanceSheetType = balanceSheetType;
        this.countryCode = countryCode;
    }
    
    public String getExposureId() {
        return exposureId;
    }
    
    public void setExposureId(String exposureId) {
        this.exposureId = exposureId;
    }
    
    public String getInstrumentId() {
        return instrumentId;
    }
    
    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }
    
    public String getInstrumentType() {
        return instrumentType;
    }
    
    public void setInstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
    }
    
    public String getCounterpartyName() {
        return counterpartyName;
    }
    
    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }
    
    public String getCounterpartyId() {
        return counterpartyId;
    }
    
    public void setCounterpartyId(String counterpartyId) {
        this.counterpartyId = counterpartyId;
    }
    
    public String getCounterpartyLei() {
        return counterpartyLei;
    }
    
    public void setCounterpartyLei(String counterpartyLei) {
        this.counterpartyLei = counterpartyLei;
    }
    
    public BigDecimal getExposureAmount() {
        return exposureAmount;
    }
    
    public void setExposureAmount(BigDecimal exposureAmount) {
        this.exposureAmount = exposureAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getProductType() {
        return productType;
    }
    
    public void setProductType(String productType) {
        this.productType = productType;
    }
    
    public String getBalanceSheetType() {
        return balanceSheetType;
    }
    
    public void setBalanceSheetType(String balanceSheetType) {
        this.balanceSheetType = balanceSheetType;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
