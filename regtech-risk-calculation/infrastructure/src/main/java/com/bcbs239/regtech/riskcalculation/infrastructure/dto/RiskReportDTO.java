package com.bcbs239.regtech.riskcalculation.infrastructure.dto;

import java.util.List;

/**
 * DTO for complete risk report containing bank info, exposures, and mitigations
 */
public record RiskReportDTO(
    BankInfoDTO bankInfo,
    List<ExposureDTO> exposures,
    List<CreditRiskMitigationDTO> creditRiskMitigation
) {}
