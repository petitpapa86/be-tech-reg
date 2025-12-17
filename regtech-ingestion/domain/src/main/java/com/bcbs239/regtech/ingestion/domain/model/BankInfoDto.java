package com.bcbs239.regtech.ingestion.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for parsing bank_info from JSON input files.
 * Maps snake_case JSON fields to Java fields.
 */
public record BankInfoDto(
    @JsonProperty("bank_name")
    @JsonAlias({"bankName", "bank_Name"})
    String bankName,

    @JsonProperty("abi_code")
    @JsonAlias({"abiCode", "abi_Code"})
    String abiCode,

    @JsonProperty("lei_code")
    @JsonAlias({"leiCode", "lei_Code"})
    String leiCode,

    @JsonProperty("report_date")
    @JsonAlias({"reportDate", "report_Date"})
    String reportDate,

    @JsonProperty("total_exposures")
    @JsonAlias({"totalExposures", "total_Exposures"})
    int totalExposures
) {}
