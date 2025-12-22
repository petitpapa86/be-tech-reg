package com.bcbs239.regtech.dataquality.domain.rules;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Domain DTO for RuleExemption (not a JPA entity).
 */
public record RuleExemptionDto(
    Long exemptionId,
    String ruleId,
    String entityType,
    String entityId,
    String exemptionReason,
    ExemptionType exemptionType,
    String approvedBy,
    LocalDate approvalDate,
    LocalDate effectiveDate,
    LocalDate expirationDate,
    Map<String, Object> conditions,
    Instant createdAt
) {
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(effectiveDate) &&
               (expirationDate == null || !today.isAfter(expirationDate));
    }
}
