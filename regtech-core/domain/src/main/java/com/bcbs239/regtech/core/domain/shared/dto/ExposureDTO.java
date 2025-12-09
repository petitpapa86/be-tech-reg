package com.bcbs239.regtech.core.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Shared DTO for exposure data across all modules.
 * Supports any type of financial instrument (LOAN, BOND, DERIVATIVE, etc.)
 */
public record ExposureDTO(
    @JsonProperty("exposure_id") String exposureId,
    @JsonProperty("instrument_id") String instrumentId,
    @JsonProperty("instrument_type") String instrumentType,
    @JsonProperty("counterparty_name") String counterpartyName,
    @JsonProperty("counterparty_id") String counterpartyId,
    @JsonProperty("counterparty_lei") String counterpartyLei,
    @JsonProperty("exposure_amount") BigDecimal exposureAmount,
    @JsonProperty("currency") String currency,
    @JsonProperty("product_type") String productType,
    @JsonProperty("balance_sheet_type") String balanceSheetType,
    @JsonProperty("country_code") String countryCode
) {}
