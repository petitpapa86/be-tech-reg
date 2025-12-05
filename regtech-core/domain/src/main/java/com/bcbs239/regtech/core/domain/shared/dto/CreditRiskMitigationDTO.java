package com.bcbs239.regtech.core.domain.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Shared DTO for credit risk mitigation data across all modules.
 */
public record CreditRiskMitigationDTO(
    @JsonProperty("exposure_id") String exposureId,
    @JsonProperty("mitigation_type") String mitigationType,
    @JsonProperty("value") BigDecimal value,
    @JsonProperty("currency") String currency
) {}
