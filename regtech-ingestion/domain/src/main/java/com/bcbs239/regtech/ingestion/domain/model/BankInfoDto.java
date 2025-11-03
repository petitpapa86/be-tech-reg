package com.bcbs239.regtech.ingestion.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankInfoDto(
    @JsonProperty("bank_name") String bankName,
    @JsonProperty("abi_code") String abiCode,
    @JsonProperty("lei_code") String leiCode,
    @JsonProperty("report_date") String reportDate,
    @JsonProperty("total_loans") int totalLoans
) {}

