package com.bcbs239.regtech.modules.ingestion.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoanExposureDto(
    @JsonProperty("loan_id") String loanId,
    @JsonProperty("exposure_id") String exposureId,
    @JsonProperty("borrower_name") String borrowerName,
    @JsonProperty("borrower_id") String borrowerId,
    @JsonProperty("counterparty_lei") String counterpartyLei,
    @JsonProperty("loan_amount") double loanAmount,
    @JsonProperty("gross_exposure_amount") double grossExposureAmount,
    @JsonProperty("net_exposure_amount") double netExposureAmount,
    @JsonProperty("currency") String currency,
    @JsonProperty("loan_type") String loanType,
    @JsonProperty("sector") String sector,
    @JsonProperty("exposure_type") String exposureType,
    @JsonProperty("borrower_country") String borrowerCountry,
    @JsonProperty("country_code") String countryCode
) {}

