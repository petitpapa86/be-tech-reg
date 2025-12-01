package com.bcbs239.regtech.riskcalculation.infrastructure.dto;

import java.math.BigDecimal;

/**
 * DTO for credit risk mitigation data
 */
public record CreditRiskMitigationDTO(
    String exposureId,
    String mitigationType,
    BigDecimal value,
    String currency
) {}
