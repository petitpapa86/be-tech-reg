package com.bcbs239.regtech.riskcalculation.infrastructure.dto;

import java.math.BigDecimal;

/**
 * DTO for generic financial exposure
 * Supports any type of financial instrument (LOAN, BOND, DERIVATIVE, etc.)
 */
public record ExposureDTO(
    String exposureId,
    String instrumentId,
    String instrumentType,
    String counterpartyName,
    String counterpartyId,
    String counterpartyLei,
    BigDecimal exposureAmount,
    String currency,
    String productType,
    String balanceSheetType,
    String countryCode
) {}
