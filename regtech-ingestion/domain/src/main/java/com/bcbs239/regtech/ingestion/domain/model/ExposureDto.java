package com.bcbs239.regtech.ingestion.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for parsing exposure data from JSON input files.
 * Maps to the standard exposure format with generic instrument fields.
 * Supports any type of financial instrument (LOAN, BOND, DERIVATIVE, etc.)
 */
public record ExposureDto(
    @JsonProperty("exposure_id") String exposureId,
    @JsonProperty("instrument_id") String instrumentId,
    @JsonProperty("instrument_type") String instrumentType,
    @JsonProperty("counterparty_name") String counterpartyName,
    @JsonProperty("counterparty_id") String counterpartyId,
    @JsonProperty("counterparty_lei") String counterpartyLei,
    @JsonProperty("exposure_amount") double exposureAmount,
    @JsonProperty("currency") String currency,
    @JsonProperty("product_type") String productType,
    @JsonProperty("balance_sheet_type") String balanceSheetType,
    @JsonProperty("country_code") String countryCode
) {}


