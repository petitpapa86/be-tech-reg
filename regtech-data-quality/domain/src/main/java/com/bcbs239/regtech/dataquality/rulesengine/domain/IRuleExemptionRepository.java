package com.bcbs239.regtech.dataquality.rulesengine.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain repository interface for rule exemptions.
 */
public interface IRuleExemptionRepository {
    
    /**
     * Finds active exemptions for a specific rule and entity.
     */
    List<RuleExemptionDto> findActiveExemptions(
        String ruleId,
        String entityType,
        String entityId,
        LocalDate currentDate
    );
}
