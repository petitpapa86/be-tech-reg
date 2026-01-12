package com.bcbs239.regtech.dataquality.domain.validation;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;

/**
 * Value object representing exposure data for validation.
 * Contains all fields needed for comprehensive data quality assessment
 * across the six quality dimensions.
 */
public record ExposureRecord(
    String exposureId,
    String counterpartyId,
    BigDecimal exposureAmount,
    String currency,
    String countryCode,
    String sector,
    String counterpartyType,
    String productType,
    String counterpartyLei,
    String internalRating,
    String riskCategory,
    BigDecimal riskWeight,
    LocalDate reportingDate,
    LocalDate valuationDate,
    LocalDate maturityDate,
    String referenceNumber,
    BigDecimal collateralValue
) {
    
    /**
     * Creates an ExposureRecord from an ExposureDTO.
     * Following DDD: the object knows how to construct itself from external data.
     * Maps available fields from DTO, leaving Data Quality-specific fields as null.
     * 
     * @param dto the ExposureDTO to convert
     * @return a new ExposureRecord instance
     */
    public static ExposureRecord fromDTO(ExposureDTO dto) {
        return ExposureRecord.builder()
            .exposureId(dto.exposureId())
            .counterpartyId(dto.counterpartyId())
            .exposureAmount(dto.exposureAmount())
            .currency(dto.currency())
            .countryCode(dto.countryCode())
            .sector(dto.sector())
            .productType(dto.productType())
            .counterpartyLei(dto.counterpartyLei())
            // Fields not available in ExposureDTO - set to null
            .counterpartyType(null)
            .internalRating(dto.internalRating())
            .riskCategory(null)
            .riskWeight(null)
            .reportingDate(null)
            .valuationDate(null)
            .maturityDate(dto.maturityDate())
            .referenceNumber(dto.instrumentId()) // Use instrumentId as reference
            .collateralValue(null) // Collateral not in DTO yet
            .build();
    }
    
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
        private BigDecimal exposureAmount;
        private String currency;
        private String countryCode;
        private String sector;
        private String counterpartyType;
        private String productType;
        private String counterpartyLei;
        private String internalRating;
        private String riskCategory;
        private BigDecimal riskWeight;
        private LocalDate reportingDate;
        private LocalDate valuationDate;
        private LocalDate maturityDate;
        private String referenceNumber;
        private BigDecimal collateralValue;
        
        public Builder exposureId(String exposureId) {
            this.exposureId = exposureId;
            return this;
        }
        
        public Builder counterpartyId(String counterpartyId) {
            this.counterpartyId = counterpartyId;
            return this;
        }
        
        public Builder exposureAmount(BigDecimal exposureAmount) {
            this.exposureAmount = exposureAmount;
            return this;
        }
        
        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }
        
        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
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
        
        public Builder counterpartyLei(String counterpartyLei) {
            this.counterpartyLei = counterpartyLei;
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
        
        public Builder collateralValue(BigDecimal collateralValue) {
            this.collateralValue = collateralValue;
            return this;
        }
        
        public ExposureRecord build() {
            return new ExposureRecord(
                exposureId,
                counterpartyId,
                exposureAmount,
                currency,
                countryCode,
                sector,
                counterpartyType,
                productType,
                counterpartyLei,
                internalRating,
                riskCategory,
                riskWeight,
                reportingDate,
                valuationDate,
                maturityDate,
                referenceNumber,
                collateralValue
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
               exposureAmount != null &&
               currency != null && !currency.trim().isEmpty() &&
               countryCode != null && !countryCode.trim().isEmpty() &&
               sector != null && !sector.trim().isEmpty();
    }
}

