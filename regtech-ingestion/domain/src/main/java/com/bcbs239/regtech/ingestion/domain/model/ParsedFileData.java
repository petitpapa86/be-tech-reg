package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain model representing parsed file data.
 * Contains the structured data extracted from uploaded files.
 */
public record ParsedFileData(
    BankInfoModel bankInfo,
    List<LoanExposure> exposures,
    List<CreditRiskMitigation> creditRiskMitigation,
    Map<String, Object> metadata
) {
    /**
     * Get the total number of exposures in this parsed data.
     */
    public int totalExposures() {
        return exposures == null ? 0 : exposures.size();
    }
    
    /**
     * Convert this domain model to a DTO for inter-module communication.
     * Following DDD: the object knows how to represent itself as a DTO.
     */
    public BatchDataDTO toDTO() {
        return new BatchDataDTO(
            bankInfo != null ? bankInfo.toDTO() : null,
            exposures != null ? exposures.stream().map(LoanExposure::toDTO).collect(Collectors.toList()) : List.of(),
            creditRiskMitigation != null ? creditRiskMitigation.stream().map(CreditRiskMitigation::toDTO).collect(Collectors.toList()) : List.of()
        );
    }
    
    /**
     * Create from DTO (factory method).
     * Following DDD: the object knows how to construct itself from a DTO.
     */
    public static ParsedFileData fromDTO(BatchDataDTO dto) {
        return new ParsedFileData(
            dto.bankInfo() != null ? BankInfoModel.fromDTO(dto.bankInfo()) : null,
            dto.exposures() != null ? dto.exposures().stream().map(LoanExposure::fromDTO).collect(Collectors.toList()) : List.of(),
            dto.creditRiskMitigation() != null ? dto.creditRiskMitigation().stream().map(CreditRiskMitigation::fromDTO).collect(Collectors.toList()) : List.of(),
            Map.of() // Empty metadata when creating from DTO
        );
    }
}
