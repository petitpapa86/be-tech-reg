package com.bcbs239.regtech.modules.dataquality.domain.validation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object representing exposure data for validation.
 * Contains all fields needed for comprehensive data quality assessment
 * across the six quality dimensions.
 */
public record ExposureRecord(
    String exposureId,
    String counterpartyId,
    BigDecimal amount,
    String currency,
    String country,
    String sector,
    String counterpartyType,
    String productType,
    String leiCode,
    String internalRating,
    String riskCategory,
    BigDecimal riskWeight,
    LocalDate reportingDate,
    LocalDate valuationDate,
    LocalDate maturityDate,
    String referenceNumber
) {
    
    /**
     * Creates a builder for constructing ExposureRecord instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for ExposureRecord
     */
    public static class Builder {
        private String exposureId;
        private String counterpartyId;
        private BigDecimal amount;
        private String currency;
        private String country;
        private String sector;
        private String counterpartyType;
        private String productType;
        private String leiCode;
        private String internalRating;
        private String riskCategory;
        private BigDecimal riskWeight;
        private LocalDate reportingDate;
        private LocalDate valuationDate;
        private LocalDate maturityDate;
        private String referenceNumber;
        
        public Builder exposureId(String exposureId) {
            this.exposureId = exposureId;
            return this;
        }
        
        public Builder counterpartyId(String counterpartyId) {
            this.counterpartyId = counterpartyId;
            return this;
        }
        
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }
        
        public Builder country(String country) {
            this.country = country;
            return this;
        }
        
        public Builder sector(String sector) {
            this.sector = sector;
            return this;
        }
        
        public Builder counterpartyType(String counterpartyType) {
            this.counterpartyType = counterpartyType;
            return this;
        }
        
        public Builder productType(String productType) {
            this.productType = productType;
            return this;
        }
        
        public Builder leiCode(String leiCode) {
            this.leiCode = leiCode;
            return this;
        }
        
        public Builder internalRating(String internalRating) {
            this.internalRating = internalRating;
            return this;
        }
        
        public Builder riskCategory(String riskCategory) {
            this.riskCategory = riskCategory;
            return this;
        }
        
        public Builder riskWeight(BigDecimal riskWeight) {
            this.riskWeight = riskWeight;
            return this;
        }
        
        public Builder reportingDate(LocalDate reportingDate) {
            this.reportingDate = reportingDate;
            return this;
        }
        
        public Builder valuationDate(LocalDate valuationDate) {
            this.valuationDate = valuationDate;
            return this;
        }
        
        public Builder maturityDate(LocalDate maturityDate) {
            this.maturityDate = maturityDate;
            return this;
        }
        
        public Builder referenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
            return this;
        }
        
        public ExposureRecord build() {
            return new ExposureRecord(
                exposureId,
                counterpartyId,
                amount,
                currency,
                country,
                sector,
                counterpartyType,
                productType,
                leiCode,
                internalRating,
                riskCategory,
                riskWeight,
                reportingDate,
                valuationDate,
                maturityDate,
                referenceNumber
            );
        }
    }
    
    /**
     * Checks if this is a corporate exposure based on sector
     */
    public boolean isCorporateExposure() {
        return sector != null && 
               (sector.startsWith("CORPORATE") || sector.equals("BANKING"));
    }
    
    /**
     * Checks if this is a term exposure (not equity)
     */
    public boolean isTermExposure() {
        return productType != null && !productType.equals("EQUITY");
    }
    
    /**
     * Checks if required fields are present (basic completeness check)
     */
    public boolean hasRequiredFields() {
        return exposureId != null && !exposureId.trim().isEmpty() &&
               amount != null &&
               currency != null && !currency.trim().isEmpty() &&
               country != null && !country.trim().isEmpty() &&
               sector != null && !sector.trim().isEmpty();
    }
}