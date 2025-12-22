package com.bcbs239.regtech.dataquality.domain.rules;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Domain DTO for BusinessRule (not a JPA entity).
 * Used to transfer business rule data between layers without coupling to infrastructure.
 */
public record BusinessRuleDto(
    String ruleId,
    String regulationId,
    String templateId,
    String ruleName,
    String ruleCode,
    String description,
    RuleType ruleType,
    String ruleCategory,
    Severity severity,
    String businessLogic,
    Integer executionOrder,
    LocalDate effectiveDate,
    LocalDate expirationDate,
    Boolean enabled,
    List<RuleParameterDto> parameters,
    Instant createdAt,
    Instant updatedAt,
    String createdBy
) {
    public boolean isApplicableOn(LocalDate date) {
        return !date.isBefore(effectiveDate) && (expirationDate == null || !date.isAfter(expirationDate));
    }

    public boolean isActive() {
        return enabled && isApplicableOn(LocalDate.now());
    }
}
