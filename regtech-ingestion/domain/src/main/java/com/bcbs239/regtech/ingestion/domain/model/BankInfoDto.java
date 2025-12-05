package com.bcbs239.regtech.ingestion.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for parsing bank_info from JSON input files.
 * Maps snake_case JSON fields to Java fields.
 */
public record BankInfoDto(
    @JsonProperty("bank_name") String bankName,
    @JsonProperty("abi_code") String abiCode,
    @JsonProperty("lei_code") String leiCode,
    @JsonProperty("report_date") String reportDate,
    @JsonProperty("total_exposures") int totalExposures
) {}
