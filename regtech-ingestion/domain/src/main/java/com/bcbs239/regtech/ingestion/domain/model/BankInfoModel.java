package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO;
import java.time.LocalDate;
import java.util.Objects;

public record BankInfoModel(
    String bankName,
    String abiCode,
    String leiCode,
    String reportDate,
    int totalLoans
) {
    public BankInfoModel {
        Objects.requireNonNull(bankName);
    }
    
    /**
     * Convert this domain model to a DTO for inter-module communication.
     * Following DDD: the object knows how to represent itself as a DTO.
     */
    public BankInfoDTO toDTO() {
        LocalDate parsedReportDate = reportDate != null ? LocalDate.parse(reportDate) : null;
        return new BankInfoDTO(
            bankName,
            abiCode,
            leiCode,
            parsedReportDate,
            totalLoans
        );
    }
    
    /**
     * Create from DTO (factory method).
     * Following DDD: the object knows how to construct itself from a DTO.
     */
    public static BankInfoModel fromDTO(BankInfoDTO dto) {
        String reportDateStr = dto.reportDate() != null ? dto.reportDate().toString() : null;
        return new BankInfoModel(
            dto.bankName(),
            dto.abiCode(),
            dto.leiCode(),
            reportDateStr,
            dto.totalExposures() != null ? dto.totalExposures() : 0
        );
    }
}

