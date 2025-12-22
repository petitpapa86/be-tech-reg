package com.bcbs239.regtech.dataquality.domain.rules;

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

    /**
     * Batch-loads all active exemptions for a given entity type and a set of entity IDs.
     *
     * <p>Implementations should include both entity-specific exemptions (entityId in the list)
     * and wildcard exemptions (entityId is null) that apply to all entities of the type.</p>
     */
    List<RuleExemptionDto> findAllActiveExemptionsForBatch(
        String entityType,
        List<String> entityIds,
        LocalDate currentDate
    );
}
