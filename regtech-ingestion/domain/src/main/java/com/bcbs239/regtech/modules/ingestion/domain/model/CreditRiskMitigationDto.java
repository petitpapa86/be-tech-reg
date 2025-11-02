package com.bcbs239.regtech.modules.ingestion.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditRiskMitigationDto(
    @JsonProperty("exposure_id") String exposureId,
    @JsonProperty("collateral_type") String collateralType,
    @JsonProperty("collateral_value") double collateralValue,
    @JsonProperty("collateral_currency") String collateralCurrency
) {}

