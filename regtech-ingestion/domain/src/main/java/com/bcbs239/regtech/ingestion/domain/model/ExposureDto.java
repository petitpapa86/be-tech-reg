package com.bcbs239.regtech.ingestion.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for parsing exposure data from JSON input files.
 * Maps to the standard exposure format with generic instrument fields.
 * Supports any type of financial instrument (LOAN, BOND, DERIVATIVE, etc.)
 */
public record ExposureDto(
    @JsonProperty("exposure_id")
    @JsonAlias({"exposureId", "exposure_Id"})
    String exposureId,

    @JsonProperty("instrument_id")
    @JsonAlias({"instrumentId", "instrument_Id"})
    String instrumentId,

    @JsonProperty("instrument_type")
    @JsonAlias({"instrumentType", "instrument_Type"})
    String instrumentType,

    @JsonProperty("counterparty_name")
    @JsonAlias({"counterpartyName", "counterparty_Name"})
    String counterpartyName,

    @JsonProperty("counterparty_id")
    @JsonAlias({"counterpartyId", "counterparty_Id"})
    String counterpartyId,

    @JsonProperty("counterparty_lei")
    @JsonAlias({"counterpartyLei", "counterparty_Lei"})
    String counterpartyLei,

    @JsonProperty("exposure_amount")
    @JsonAlias({"exposureAmount", "exposure_Amount"})
    double exposureAmount,

    @JsonProperty("currency")
    @JsonAlias({"Currency"})
    String currency,

    @JsonProperty("product_type")
    @JsonAlias({"productType", "product_Type"})
    String productType,

    @JsonProperty("sector")
    @JsonAlias({"Sector"})
    String sector,

    @JsonProperty("maturity_date")
    @JsonAlias({"maturityDate", "maturity_Date"})
    String maturityDate,

    @JsonProperty("balance_sheet_type")
    @JsonAlias({"balanceSheetType", "balance_Sheet_Type"})
    String balanceSheetType,

    @JsonProperty("country_code")
    @JsonAlias({"countryCode", "country_Code"})
    String countryCode,

    @JsonProperty("internal_rating")
    @JsonAlias({"internalRating", "internal_Rating"})
    String internalRating
) {}


