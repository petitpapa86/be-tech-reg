package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import java.math.BigDecimal;
import java.util.Objects;

public record LoanExposure(
    String loanId,
    String exposureId,
    String borrowerName,
    String borrowerId,
    String counterpartyLei,
    double loanAmount,
    double grossExposureAmount,
    double netExposureAmount,
    String currency,
    String loanType,
    String sector,
    String exposureType,
    String borrowerCountry,
    String countryCode
) {
    public LoanExposure {
        Objects.requireNonNull(loanId);
    }

    // toString kept for readability
    @Override
    public String toString() {
        return "LoanExposure{" + "loanId='" + loanId + '\'' + ", exposureId='" + exposureId + '\'' + '}';
    }
    
    /**
     * Convert to DTO for inter-module communication.
     * Following DDD: the object knows how to represent itself as a DTO.
     * 
     * Maps LoanExposure fields to ExposureDTO:
     * - loanId -> instrumentId
     * - exposureId -> exposureId
     * - borrowerName -> counterpartyName
     * - borrowerId -> counterpartyId
     * - counterpartyLei -> counterpartyLei
     * - grossExposureAmount -> exposureAmount
     * - currency -> currency
     * - loanType -> productType
     * - exposureType -> balanceSheetType
     * - countryCode -> countryCode
     * - "LOAN" -> instrumentType (constant)
     */
    public ExposureDTO toDTO() {
        return new ExposureDTO(
            exposureId,
            loanId,                                    // instrumentId
            "LOAN",                                    // instrumentType (constant for loans)
            borrowerName,                              // counterpartyName
            borrowerId,                                // counterpartyId
            counterpartyLei,                           // counterpartyLei
            BigDecimal.valueOf(grossExposureAmount),   // exposureAmount
            currency,                                  // currency
            loanType,                                  // productType
            exposureType,                              // balanceSheetType
            countryCode                                // countryCode
        );
    }
    
    /**
     * Create from DTO (factory method).
     * Following DDD: the object knows how to construct itself from a DTO.
     */
    public static LoanExposure fromDTO(ExposureDTO dto) {
        return new LoanExposure(
            dto.instrumentId(),                        // loanId
            dto.exposureId(),                          // exposureId
            dto.counterpartyName(),                    // borrowerName
            dto.counterpartyId(),                      // borrowerId
            dto.counterpartyLei(),                     // counterpartyLei
            dto.exposureAmount().doubleValue(),        // loanAmount (using exposureAmount)
            dto.exposureAmount().doubleValue(),        // grossExposureAmount
            dto.exposureAmount().doubleValue(),        // netExposureAmount (same as gross for now)
            dto.currency(),                            // currency
            dto.productType(),                         // loanType
            null,                                      // sector (not in DTO)
            dto.balanceSheetType(),                    // exposureType
            null,                                      // borrowerCountry (not in DTO)
            dto.countryCode()                          // countryCode
        );
    }
}

