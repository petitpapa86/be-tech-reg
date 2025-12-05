package com.bcbs239.regtech.core.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Shared DTO for complete batch data across all modules.
 * This is the root object for batch data files stored in S3.
 */
public record BatchDataDTO(
    @JsonProperty("bank_info") BankInfoDTO bankInfo,
    @JsonProperty("exposures") List<ExposureDTO> exposures,
    @JsonProperty("credit_risk_mitigation") List<CreditRiskMitigationDTO> creditRiskMitigation
) {}
