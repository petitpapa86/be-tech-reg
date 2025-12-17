package com.bcbs239.regtech.ingestion.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreditRiskMitigationDto(
    @JsonProperty("exposure_id")
    @JsonAlias({"exposureId", "exposure_Id"})
    String exposureId,

    @JsonProperty("collateral_type")
    @JsonAlias({"collateralType", "collateral_Type"})
    String collateralType,

    @JsonProperty("collateral_value")
    @JsonAlias({"collateralValue", "collateral_Value"})
    double collateralValue,

    @JsonProperty("collateral_currency")
    @JsonAlias({"collateralCurrency", "collateral_Currency"})
    String collateralCurrency
) {}


