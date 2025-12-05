package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import java.math.BigDecimal;
import java.util.Objects;

public record CreditRiskMitigation(
    String exposureId,
    String collateralType,
    double collateralValue,
    String collateralCurrency
) {
    public CreditRiskMitigation {
        Objects.requireNonNull(exposureId);
    }
    
    /**
     * Convert to DTO for inter-module communication.
     * Following DDD: the object knows how to represent itself as a DTO.
     * 
     * Maps CreditRiskMitigation fields to CreditRiskMitigationDTO:
     * - exposureId -> exposureId
     * - collateralType -> mitigationType
     * - collateralValue -> value
     * - collateralCurrency -> currency
     */
    public CreditRiskMitigationDTO toDTO() {
        return new CreditRiskMitigationDTO(
            exposureId,
            collateralType,                            // mitigationType
            BigDecimal.valueOf(collateralValue),       // value
            collateralCurrency                         // currency
        );
    }
    
    /**
     * Create from DTO (factory method).
     * Following DDD: the object knows how to construct itself from a DTO.
     */
    public static CreditRiskMitigation fromDTO(CreditRiskMitigationDTO dto) {
        return new CreditRiskMitigation(
            dto.exposureId(),
            dto.mitigationType(),                      // collateralType
            dto.value().doubleValue(),                 // collateralValue
            dto.currency()                             // collateralCurrency
        );
    }
}

