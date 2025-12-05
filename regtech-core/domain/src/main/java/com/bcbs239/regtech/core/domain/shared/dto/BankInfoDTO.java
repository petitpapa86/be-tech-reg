package com.bcbs239.regtech.core.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * Shared DTO for bank information across all modules.
 * Used for inter-module communication via JSON serialization.
 */
public record BankInfoDTO(
    @JsonProperty("bank_name") String bankName,
    @JsonProperty("abi_code") String abiCode,
    @JsonProperty("lei_code") String leiCode,
    @JsonProperty("report_date") LocalDate reportDate,
    @JsonProperty("total_exposures") Integer totalExposures
) {}
